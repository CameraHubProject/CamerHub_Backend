package vn.camerahub.identity;

import java.util.*;
import java.time.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import vn.camerahub.identity.config.*;
import vn.camerahub.identity.controller.IdentityController;
import vn.camerahub.identity.entity.Account;
import vn.camerahub.identity.mapper.ProfileMapper;
import vn.camerahub.identity.repository.*;
import vn.camerahub.identity.service.*;
import vn.camerahub.marketplace.controller.MarketplaceController;
import vn.camerahub.marketplace.repository.ProductRepository;
import vn.camerahub.marketplace.service.*;
import vn.camerahub.shared.*;
import vn.camerahub.shared.config.JwtUtil;
import vn.camerahub.shared.exception.ApiExceptionWriter;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers={IdentityController.class, MarketplaceController.class})
@org.springframework.test.context.ContextConfiguration(classes=AccountFeaturesHttpTest.Beans.class)
@Import({IdentityController.class, MarketplaceController.class, vn.camerahub.shared.exception.GlobalExceptionHandler.class, vn.camerahub.shared.dto.ApiResponseAdvice.class, SecurityConfig.class, AccountService.class, AvatarService.class, MarketplaceService.class, ListingLifecycleService.class, ApiExceptionWriter.class, AccountFeaturesHttpTest.Beans.class})
class AccountFeaturesHttpTest {
    @Configuration static class Beans {
        @Bean Clock clock() { return Clock.systemUTC(); }
        @Bean ProfileMapper mapper() { return org.mapstruct.factory.Mappers.getMapper(ProfileMapper.class); }
    }
    @Autowired MockMvc mvc;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwords;
    @MockitoBean AccountRepository accounts;
    @MockitoBean UserRoleJpaRepository roles;
    @MockitoBean AuditLogJpaRepository audit;
    @MockitoBean IntegrationEvents events;
    @MockitoBean ProductRepository products;
    @MockitoBean ListingSubmission submission;
    @MockitoBean InternalClient client;
    @MockitoBean AuthTokenService tokens;
    @MockitoBean JwtUtil jwt;
    @MockitoBean GoogleOidcUserService google;
    @MockitoBean GoogleLoginHandlers handlers;
    UUID owner=UUID.randomUUID();
    Account account;
    @BeforeEach void setup() {
        account=new Account(); account.setId(owner); account.setFullName("Owner"); account.setEmail("owner@example.test");
        account.setPhone("0912345678"); account.setAccountStatus("ACTIVE"); account.setProfileVersion(1L);
        account.setPasswordHash(passwords.encode("old-password-123"));
        when(accounts.findById(owner)).thenReturn(Optional.of(account));
        when(accounts.lockById(owner)).thenReturn(Optional.of(account));
        when(roles.findActiveRoles(owner)).thenReturn(List.of("USER"));
        var token=Jwt.withTokenValue("test").header("alg","RS256").subject(owner.toString()).build();
        when(jwt.decode("test")).thenReturn(token); when(tokens.verify(token)).thenReturn(List.of("USER"));
    }
    @Test void allFeatureEndpointsRequireLogin() throws Exception {
        for (String path : List.of("/api/auth/me", "/api/auth/me/password", "/api/marketplace/listings/"+UUID.randomUUID()+"/draft"))
            mvc.perform(post(path).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/auth/me/avatar").file(new MockMultipartFile("file","a.png","image/png",new byte[]{1})).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me/avatar").param("key",UUID.randomUUID().toString())).andExpect(status().isUnauthorized());
    }
    @Test void profileUpdateIsBoundToTokenOwnerAndValidates() throws Exception {
        mvc.perform(post("/api/auth/me").header("Authorization","Bearer test").contentType("application/json").content("{\"fullName\":\"New Name\",\"phone\":\"0912345678\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(owner.toString())).andExpect(jsonPath("$.data.fullName").value("New Name"));
        mvc.perform(post("/api/auth/me").header("Authorization","Bearer test").contentType("application/json").content("{\"fullName\":\"\",\"phone\":\"abc\"}"))
            .andExpect(status().isBadRequest());
    }
    @Test void cannotTargetAnotherProfileOrChangePrivilegedFields() throws Exception {
        for(String field:List.of("id","userId","email","username","role","roles"))
            mvc.perform(post("/api/auth/me").header("Authorization","Bearer test").contentType("application/json").content("{\"fullName\":\"Hacker\",\"phone\":\"0912345678\",\""+field+"\":\"other\"}"))
                .andExpect(status().isBadRequest());
        assertThat(account.getFullName()).isEqualTo("Owner"); verify(accounts,never()).saveAndFlush(any());
    }
    @Test void listingEditingRejectsAnotherAuthorMissingAndInvalidContent() throws Exception {
        UUID id=UUID.randomUUID(); var product=new vn.camerahub.marketplace.entity.Product(); product.setSellerId(UUID.randomUUID());
        when(products.findById(id)).thenReturn(Optional.of(product));
        mvc.perform(get("/api/marketplace/listings/"+id+"/draft").header("Authorization","Bearer test")).andExpect(status().isForbidden());
        mvc.perform(get("/api/marketplace/listings/"+UUID.randomUUID()+"/draft").header("Authorization","Bearer test")).andExpect(status().isNotFound());
        mvc.perform(post("/api/marketplace/listings/"+id+"/draft").header("Authorization","Bearer test").contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
    }
    @Test void listingWithdrawalValidatesReasonAndBindsToSignedInOwner() throws Exception {
        UUID id=UUID.randomUUID();
        when(products.lockListingLifecycle(id,owner)).thenReturn(List.of(Map.of("listing_status","PUBLISHED")));
        for(String action:List.of("hide","delete")) {
            mvc.perform(post("/api/marketplace/listings/"+id+"/"+action).header("Authorization","Bearer test")
                    .contentType("application/json").content("{\"reason\":\"   \"}"))
                    .andExpect(status().isBadRequest());
        }
        verify(products,never()).withdrawListing(any(),any(),any(),any());
        mvc.perform(post("/api/marketplace/listings/"+id+"/hide").header("Authorization","Bearer test")
                .contentType("application/json").content("{\"reason\":\" Tạm ngừng bán \"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.listingStatus").value("HIDDEN"));
        verify(products).withdrawListing("Tạm ngừng bán",false,id,owner);
    }
    @Test void listingDeletionRequiresLoginAndCannotTargetAnotherOwnersListing() throws Exception {
        UUID id=UUID.randomUUID();
        mvc.perform(post("/api/marketplace/listings/"+id+"/delete").with(csrf())
                .contentType("application/json").content("{\"reason\":\"Đăng nhầm\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/marketplace/listings/"+id+"/delete").header("Authorization","Bearer test")
                .contentType("application/json").content("{\"reason\":\"Đăng nhầm\"}"))
                .andExpect(status().isNotFound());
        verify(products,never()).withdrawListing(any(),any(),any(),any());
    }
    @Test void avatarUploadReplacesOldBytesGeneratesKeyAndServesImmediately() throws Exception {
        account.setAvatarKey(UUID.randomUUID()); UUID old=account.getAvatarKey(); account.setAvatarContent(new byte[]{9});
        var out=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",out);
        mvc.perform(multipart("/api/auth/me/avatar").file(new MockMultipartFile("file","../../evil.png","image/png",out.toByteArray())).header("Authorization","Bearer test"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.avatarUrl").exists()).andExpect(jsonPath("$.data.avatarContent").doesNotExist());
        assertThat(account.getAvatarKey()).isNotEqualTo(old); assertThat(account.getAvatarContent()).isEqualTo(out.toByteArray());
        mvc.perform(get("/api/auth/me/avatar").param("key",account.getAvatarKey().toString()).header("Authorization","Bearer test"))
            .andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(content().bytes(out.toByteArray()));
        mvc.perform(get("/api/auth/me/avatar").param("key",old.toString()).header("Authorization","Bearer test")).andExpect(status().isNotFound());
    }
    @Test void acceptsJpegAndWebpContent() throws Exception {
        var out=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"jpeg",out);
        mvc.perform(multipart("/api/auth/me/avatar").file(new MockMultipartFile("file","avatar.jpg","image/jpeg",out.toByteArray())).header("Authorization","Bearer test")).andExpect(status().isOk());
        byte[] webp=Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");
        mvc.perform(multipart("/api/auth/me/avatar").file(new MockMultipartFile("file","avatar.webp","image/webp",webp)).header("Authorization","Bearer test")).andExpect(status().isOk());
    }
    @Test void missingAvatarAndOtherProfileRouteAreRejected() throws Exception {
        mvc.perform(multipart("/api/auth/me/avatar").header("Authorization","Bearer test")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/users/"+UUID.randomUUID()).header("Authorization","Bearer test").contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        verify(accounts,never()).saveAndFlush(any());
    }
    @Test void authorCanEditPublishedListingThroughValidatedEndpoint() throws Exception {
        UUID id=UUID.randomUUID(), model=UUID.randomUUID();
        var product=new vn.camerahub.marketplace.entity.Product(); product.setSellerId(owner);
        when(products.findById(id)).thenReturn(Optional.of(product));
        when(products.lockEditableDraft(id,owner)).thenReturn(List.of(Map.of("listing_status","PUBLISHED","model_id",model,"inspection_badge","NOT_INSPECTED")));
        when(products.countModel(Integer.class,model)).thenReturn(1);
        String body="{\"modelId\":\""+model+"\",\"title\":\"New title\",\"priceVnd\":\"100000\",\"description\":\"New description\",\"conditionNotes\":\"Good\",\"purposes\":[]}";
        mvc.perform(post("/api/marketplace/listings/"+id+"/draft").header("Authorization","Bearer test").contentType("application/json").content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.listingStatus").value("NEEDS_REVISION"));
        verify(products).restoreDraftStatus("NEEDS_REVISION",id);
        product.setSellerId(UUID.randomUUID());
        mvc.perform(post("/api/marketplace/listings/"+id+"/draft").header("Authorization","Bearer test").contentType("application/json").content(body)).andExpect(status().isForbidden());
    }
    @Test void avatarRejectsWrongFormatOversizeEmptyAndSpoofedMime() throws Exception {
        for(var file:List.of(new MockMultipartFile("file","a.gif","image/gif",new byte[]{1}),
            new MockMultipartFile("file","a.png","image/png",new byte[2097153]),
            new MockMultipartFile("file","a.png","image/png",new byte[0]),
            new MockMultipartFile("file","a.png","image/png","not-an-image".getBytes()),
            new MockMultipartFile("file","a.webp","image/webp","RIFF1234WEBPbad".getBytes())))
            mvc.perform(multipart("/api/auth/me/avatar").file(file).header("Authorization","Bearer test")).andExpect(status().isBadRequest());
        verify(accounts,never()).saveAndFlush(any());
    }
    @Test void passwordSuccessAndValidationNeverReturnSecrets() throws Exception {
        mvc.perform(post("/api/auth/me/password").header("Authorization","Bearer test").contentType("application/json")
            .content("{\"currentPassword\":\"old-password-123\",\"newPassword\":\"new-password-456\",\"confirmPassword\":\"new-password-456\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("password-"))));
        for(String body:List.of("{\"currentPassword\":\"wrong\",\"newPassword\":\"new-password-789\",\"confirmPassword\":\"new-password-789\"}",
            "{\"currentPassword\":\"new-password-456\",\"newPassword\":\"short\",\"confirmPassword\":\"short\"}"))
            mvc.perform(post("/api/auth/me/password").header("Authorization","Bearer test").contentType("application/json").content(body)).andExpect(status().isBadRequest());
    }
}
