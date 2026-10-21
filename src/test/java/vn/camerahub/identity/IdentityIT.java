package vn.camerahub.identity;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Explicit database opt-in; fixtures and uploaded evidence are rolled back after each test. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class IdentityIT {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired PasswordEncoder passwords;
    @Autowired AccountService accounts;
    private static final String PASSWORD = "fixture-only-password-2026";

    @Test
    void registrationValidatesInputNormalizesEmailAndNeverAcceptsRoles() throws Exception {
        String email = UUID.randomUUID() + "@example.invalid";
        mvc.perform(post("/api/auth/register").contentType("application/json").content(registration(email)))
                .andExpect(status().isForbidden());
        var result = mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(registration(email.toUpperCase())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles[0]").value("USER")).andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("password", "ADMIN", "SELLER", "password_hash");
        String hash = jdbc.queryForObject("SELECT password_hash FROM camerahub.users WHERE email=?", String.class, email);
        assertThat(hash).isNotEqualTo(PASSWORD);
        assertThat(passwords.matches(PASSWORD, hash)).isTrue();
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(registration(email)))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidRegistrationAndMultibytePasswordAreRejected() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                .content("{\"email\":\"bad\",\"password\":\"short\",\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest());
        String payload = json.writeValueAsString(java.util.Map.of("email", UUID.randomUUID()+"@example.invalid", "password", "ắ".repeat(30), "fullName", "Test"));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json").content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void realCsrfSessionLoginRotationLogoutAndAccountSuspension() throws Exception {
        var user = fixture();
        var csrfResult = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) csrfResult.getRequest().getSession(false);
        String oldId = session.getId();
        String oldToken = json.readTree(csrfResult.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-TOKEN",oldToken)
                .param("email",user.email()).param("password",PASSWORD)).andExpect(status().isNoContent());
        assertThat(session.getId()).isNotEqualTo(oldId);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(user.id().toString()));
        mvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-TOKEN",oldToken)).andExpect(status().isForbidden());
        String newToken = json.readTree(mvc.perform(get("/api/auth/csrf").session(session)).andReturn().getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/api/auth/logout").session(session).header("X-CSRF-TOKEN",newToken)).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        var suspendedSession = login(user);
        jdbc.update("UPDATE camerahub.users SET account_status='SUSPENDED' WHERE id=?",user.id());
        mvc.perform(get("/api/auth/me").session(suspendedSession)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).param("email",user.email()).param("password",PASSWORD)).andExpect(status().isUnauthorized());
    }

    @Test
    void sellerRevisionApprovalOwnershipAndRoleRefresh() throws Exception {
        var seller = fixture(); var admin = fixture(); var stranger = fixture();
        jdbc.update("INSERT INTO camerahub.user_roles(user_id,role_code) VALUES (?,'ADMIN')",admin.id());
        var sellerSession = login(seller); var adminSession = login(admin);
        var submitted = mvc.perform(multipart("/api/seller-verification").file(pdf()).session(sellerSession).with(csrf()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING")).andReturn();
        String submittedBody = submitted.getResponse().getContentAsString();
        assertThat(submittedBody).doesNotContain("evidence_private_key", "storage", ".pdf");
        UUID verification = UUID.fromString(json.readTree(submittedBody).get("id").asText());
        mvc.perform(multipart("/api/seller-verification").file(pdf()).session(sellerSession).with(csrf())).andExpect(status().isConflict());
        mvc.perform(get("/api/seller-verification/{id}/evidence", verification).session(login(stranger))).andExpect(status().isNotFound());
        mvc.perform(get("/api/seller-verification/{id}/evidence", verification)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/seller-verification/{id}/evidence", verification).session(adminSession))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(header().string("Content-Disposition","attachment; filename=\"ho-so.pdf\""));
        review(sellerSession,verification,"VERIFIED",403);
        review(adminSession,verification,"NEEDS_REVISION",200);
        mvc.perform(multipart("/api/seller-verification").file(pdf()).session(sellerSession).with(csrf()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(verification.toString())).andExpect(jsonPath("$.status").value("PENDING"));
        review(adminSession,verification,"VERIFIED",200);
        mvc.perform(get("/api/auth/me").session(sellerSession)).andExpect(status().isOk()).andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.hasItems("USER","SELLER")));
        review(adminSession,verification,"REJECTED",409);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM camerahub.audit_logs WHERE entity_id=? AND action='REVIEW'",Integer.class,verification)).isEqualTo(2);
        jdbc.update("DELETE FROM camerahub.user_roles WHERE user_id=? AND role_code='ADMIN'",admin.id());
        mvc.perform(get("/api/admin/seller-verifications").session(adminSession)).andExpect(status().isForbidden());
    }

    @Test
    void adminCannotSelfApproveAndBadEvidenceIsRejected() throws Exception {
        var admin = fixture();
        jdbc.update("INSERT INTO camerahub.user_roles(user_id,role_code) VALUES (?,'ADMIN')",admin.id());
        var session = login(admin);
        mvc.perform(multipart("/api/seller-verification").file(new MockMultipartFile("file","pretend.png","image/png","<script>bad</script>".getBytes(StandardCharsets.UTF_8)))
                .session(session).with(csrf())).andExpect(status().isBadRequest());
        var result = mvc.perform(multipart("/api/seller-verification").file(pdf()).session(session).with(csrf())).andExpect(status().isCreated()).andReturn();
        UUID id = UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
        review(session,id,"VERIFIED",403);
        mvc.perform(post("/api/admin/seller-verifications/{id}/review",id).session(session).with(csrf())
                .contentType("application/json").content("{\"decision\":null,\"note\":\"test\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void rejectedSellerCanSubmitANewApplication() throws Exception {
        var seller=fixture(); var admin=fixture();
        jdbc.update("INSERT INTO camerahub.user_roles(user_id,role_code) VALUES (?,'ADMIN')",admin.id());
        var sellerSession=login(seller);
        var result=mvc.perform(multipart("/api/seller-verification").file(pdf()).session(sellerSession).with(csrf())).andExpect(status().isCreated()).andReturn();
        UUID id=UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
        review(login(admin),id,"REJECTED",200);
        mvc.perform(multipart("/api/seller-verification").file(pdf()).session(sellerSession).with(csrf())).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(id.toString())));
    }

    private AccountService.Account fixture() { return accounts.register(UUID.randomUUID()+"@example.invalid",PASSWORD,"Integration fixture",null); }
    private MockHttpSession login(AccountService.Account user) throws Exception {
        return (MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).param("email",user.email()).param("password",PASSWORD))
                .andExpect(status().isNoContent()).andReturn().getRequest().getSession(false);
    }
    private MockMultipartFile pdf() { return new MockMultipartFile("file","fixture.pdf","application/pdf","%PDF-1.4\nfixture-only".getBytes(StandardCharsets.UTF_8)); }
    private void review(MockHttpSession session,UUID id,String decision,int expected) throws Exception {
        mvc.perform(post("/api/admin/seller-verifications/{id}/review",id).session(session).with(csrf()).contentType("application/json")
                .content(json.writeValueAsString(java.util.Map.of("decision",decision,"note","Integration decision")))).andExpect(status().is(expected));
    }
    private String registration(String email) throws Exception {
        return json.writeValueAsString(java.util.Map.of("email",email,"password",PASSWORD,"fullName","Integration fixture","roles",java.util.List.of("ADMIN","SELLER")));
    }
}
