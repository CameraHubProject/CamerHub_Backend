package vn.camerahub.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises the real authorization-code filter and signed OIDC validation against a loopback provider.
 * Database fixtures roll back; no real Google accounts, codes or tokens are used. */
@SpringBootTest(properties={"camerahub.google.enabled=true", "camerahub.google.client-id=fixture-client", "camerahub.google.client-secret=fixture-secret"})
@AutoConfigureMockMvc
@Import(GoogleLoginIT.Provider.class)
@Transactional
class GoogleLoginIT {
    static final ObjectMapper JSON = new ObjectMapper();
    static final RSAKey KEY;
    static final HttpServer SERVER;
    static final String ISSUER;
    static String subject, email, nonce, audience;
    static boolean verified;
    static {
        try {
            KEY = new RSAKeyGenerator(2048).keyID("fixture-key").generate();
            SERVER = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            ISSUER = "http://127.0.0.1:" + SERVER.getAddress().getPort();
            SERVER.createContext("/jwks", exchange -> {
                byte[] body = JSON.writeValueAsBytes(Map.of("keys",List.of(KEY.toPublicJWK().toJSONObject())));
                exchange.getResponseHeaders().set("Content-Type","application/json");
                exchange.sendResponseHeaders(200,body.length); exchange.getResponseBody().write(body); exchange.close();
            });
            SERVER.createContext("/token", exchange -> {
                try {
                    var jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("fixture-key").type(JOSEObjectType.JWT).build(),
                        new JWTClaimsSet.Builder().issuer(ISSUER).subject(subject).audience(audience)
                            .issueTime(Date.from(Instant.now())).expirationTime(Date.from(Instant.now().plusSeconds(300)))
                            .claim("nonce",nonce).claim("email",email).claim("email_verified",verified).claim("name","Google Fixture").build());
                    jwt.sign(new RSASSASigner(KEY));
                    byte[] body = JSON.writeValueAsBytes(Map.of("access_token","fixture-access-token","token_type","Bearer","expires_in",300,"id_token",jwt.serialize(),"scope","openid email profile"));
                    exchange.getResponseHeaders().set("Content-Type","application/json");
                    exchange.sendResponseHeaders(200,body.length); exchange.getResponseBody().write(body);
                } catch(Exception exception) { exchange.sendResponseHeaders(500,-1); }
                finally { exchange.close(); }
            });
            SERVER.createContext("/userinfo", exchange -> {
                byte[] body = JSON.writeValueAsBytes(Map.of("sub",subject,"email",email,"email_verified",verified,"name","Google Fixture"));
                exchange.getResponseHeaders().set("Content-Type","application/json");
                exchange.sendResponseHeaders(200,body.length); exchange.getResponseBody().write(body); exchange.close();
            });
            SERVER.start();
        } catch(Exception exception) { throw new ExceptionInInitializerError(exception); }
    }
    @AfterAll static void stopProvider() { SERVER.stop(0); }
    @TestConfiguration
    static class Provider {
        @Bean @Primary ClientRegistrationRepository fixtureGoogle() {
            return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("google")
                .clientId("fixture-client").clientSecret("fixture-secret").authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/api/auth/google/callback").scope("openid","email","profile")
                .authorizationUri(ISSUER+"/authorize").tokenUri(ISSUER+"/token").jwkSetUri(ISSUER+"/jwks")
                .issuerUri(ISSUER).userInfoUri(ISSUER+"/userinfo").userNameAttributeName("sub").clientName("Google Fixture").build());
        }
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired AccountService accounts;
    @BeforeEach void claims() { subject=UUID.randomUUID().toString(); email=UUID.randomUUID()+"@example.invalid"; verified=true; audience="fixture-client"; }

    record Start(MockHttpSession session, String state) {}
    Start start() throws Exception {
        var result=mvc.perform(get("/api/auth/oauth2/google")).andExpect(status().is3xxRedirection()).andReturn();
        Map<String,String> query=new HashMap<>();
        for(String part:URI.create(result.getResponse().getRedirectedUrl()).getRawQuery().split("&")) {
            var pair=part.split("=",2); query.put(pair[0],URLDecoder.decode(pair[1],StandardCharsets.UTF_8));
        }
        nonce=query.get("nonce"); assertThat(nonce).isNotBlank();
        assertThat(query.get("scope")).contains("openid","email","profile").doesNotContain("gmail");
        return new Start((MockHttpSession)result.getRequest().getSession(false),query.get("state"));
    }
    void callback(Start start, String target) throws Exception {
        mvc.perform(get("/api/auth/google/callback").session(start.session()).param("state",start.state()).param("code","fixture-code"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("http://127.0.0.1:5173"+target));
    }

    @Test void newGoogleAccountReturnsToExistingSessionApiAndLogsOut() throws Exception {
        var start=start(); String oldId=start.session().getId(); callback(start,"/tai-khoan");
        assertThat(start.session().getId()).isNotEqualTo(oldId);
        var body=mvc.perform(get("/api/auth/me").session(start.session())).andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.roles[0]").value("USER")).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("access_token","id_token","password","subject","ADMIN","SELLER");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM camerahub.auth_identities WHERE subject=?",Integer.class,subject)).isEqualTo(1);
        var id=jdbc.queryForObject("SELECT user_id FROM camerahub.auth_identities WHERE subject=?",UUID.class,subject);
        assertThat(jdbc.queryForObject("SELECT email_verified_at IS NOT NULL FROM camerahub.users WHERE id=?",Boolean.class,id)).isTrue();
        // Identity is the immutable Google subject, even if Google changes the email claim later.
        email=UUID.randomUUID()+"@example.invalid"; var again=start(); callback(again,"/tai-khoan");
        mvc.perform(get("/api/auth/me").session(again.session())).andExpect(jsonPath("$.id").value(id.toString()));
        mvc.perform(post("/api/auth/logout").session(again.session()).with(csrf())).andExpect(status().isNoContent());
        assertThat(again.session().isInvalid()).isTrue();
    }

    @Test void existingEmailRequiresPasswordAndRetainsExistingUserAndRoles() throws Exception {
        var existing=accounts.register(email,"existing-password-2026","Existing",null);
        jdbc.update("INSERT INTO camerahub.user_roles(user_id,role_code) VALUES (?,'ADMIN')",existing.id());
        var start=start(); callback(start,"/dang-nhap?google=link_required");
        mvc.perform(get("/api/auth/me").session(start.session())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/google/status").session(start.session())).andExpect(jsonPath("$.pendingEmail").value(email));
        mvc.perform(post("/api/auth/google/link").session(start.session()).contentType("application/json").content("{\"password\":\"existing-password-2026\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/google/link").session(start.session()).with(csrf()).contentType("application/json").content("{\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM camerahub.auth_identities WHERE subject=?",Integer.class,subject)).isZero();
        String oldId=start.session().getId();
        mvc.perform(post("/api/auth/google/link").session(start.session()).with(csrf()).contentType("application/json").content("{\"password\":\"existing-password-2026\"}"))
            .andExpect(status().isNoContent());
        assertThat(start.session().getId()).isNotEqualTo(oldId);
        mvc.perform(get("/api/auth/me").session(start.session())).andExpect(jsonPath("$.id").value(existing.id().toString()))
            .andExpect(jsonPath("$.roles").isArray());
        assertThat(accounts.get(existing.id()).roles()).contains("ADMIN");
    }

    @Test void badStateAndUnsolicitedCallbacksDoNotCreateAccounts() throws Exception {
        var start=start();
        mvc.perform(get("/api/auth/google/callback").session(start.session()).param("state","wrong-state").param("code","fixture-code"))
            .andExpect(redirectedUrl("http://127.0.0.1:5173/dang-nhap?google=failed"));
        mvc.perform(get("/api/auth/google/callback").param("state","arbitrary").param("code","fixture-code"))
            .andExpect(redirectedUrl("http://127.0.0.1:5173/dang-nhap?google=failed"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM camerahub.users WHERE email=?",Integer.class,email)).isZero();
    }

    @Test void badAudienceAndNonceAreRejectedBeforeCreatingAccounts() throws Exception {
        var start=start(); audience="another-client"; callback(start,"/dang-nhap?google=failed");
        audience="fixture-client"; var next=start(); nonce="wrong-nonce"; callback(next,"/dang-nhap?google=failed");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM camerahub.users WHERE email=?",Integer.class,email)).isZero();
    }

    @Test void unverifiedAndSuspendedAccountsCannotSignIn() throws Exception {
        var start=start(); verified=false; callback(start,"/dang-nhap?google=unverified");
        verified=true; var next=start(); callback(next,"/tai-khoan");
        jdbc.update("UPDATE camerahub.users SET account_status='SUSPENDED' WHERE email=?",email);
        var blocked=start(); callback(blocked,"/dang-nhap?google=inactive");
        mvc.perform(get("/api/auth/me").session(next.session())).andExpect(status().isUnauthorized());
    }

    @Test void linkProofExpiresAndLimitsPasswordAttempts() throws Exception {
        accounts.register(email,"existing-password-2026","Existing",null);
        var start=start(); callback(start,"/dang-nhap?google=link_required");
        for(int i=0;i<5;i++) mvc.perform(post("/api/auth/google/link").session(start.session()).with(csrf())
            .contentType("application/json").content("{\"password\":\"wrong-password\"}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/google/status").session(start.session())).andExpect(jsonPath("$.pendingEmail").isEmpty());
        var next=start(); callback(next,"/dang-nhap?google=link_required");
        next.session().setAttribute(GoogleLoginHandlers.PENDING,new GoogleAccountService.PendingLink(
            GoogleAccountService.verified(subject,email,true,"Fixture"),Instant.now().minusSeconds(1),0));
        mvc.perform(post("/api/auth/google/link").session(next.session()).with(csrf()).contentType("application/json")
            .content("{\"password\":\"existing-password-2026\"}")).andExpect(status().isBadRequest());
    }
}
