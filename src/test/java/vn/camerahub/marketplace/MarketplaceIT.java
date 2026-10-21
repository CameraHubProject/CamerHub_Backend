package vn.camerahub.marketplace;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MarketplaceIT {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    private final MarketplaceController.Address address=new MarketplaceController.Address("Fixture buyer","0900000000","Test province","Test street",false);
    UUID account() {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO camerahub.users(id,email,password_hash,full_name) VALUES (?,?,?,'Marketplace fixture')",id,id+"@example.invalid","not-a-login");
        jdbc.update("INSERT INTO camerahub.user_roles(user_id,role_code) VALUES (?,'BUYER')",id);
        return id;
    }
    UUID product(UUID seller,long price) {
        UUID brand=UUID.randomUUID(),model=UUID.randomUUID(),id=UUID.randomUUID();
        jdbc.update("INSERT INTO camerahub.brands(id,name) VALUES (?,?)",brand,"Fixture "+brand);
        jdbc.update("INSERT INTO camerahub.camera_models(id,brand_id,model_name,device_type) VALUES (?,?,'Fixture','BODY')",model,brand);
        jdbc.update("INSERT INTO camerahub.products(id,seller_id,model_id,title,price_vnd,description,condition_notes,listing_status,serial_number_private) VALUES (?,?,?,'Marketplace fixture',?,'Fixture description','Fixture condition','PUBLISHED','PRIVATE-SERIAL')",id,seller,model,price);
        return id;
    }
    String checkout(UUID buyer,UUID product,UUID request) throws Exception {
        return mvc.perform(post("/api/marketplace/checkout").with(user(buyer.toString()).roles("USER")).with(csrf())
            .contentType("application/json").content(json.writeValueAsString(new MarketplaceController.Checkout(request,List.of(product),"DELIVERY",address))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
    @Test void checkoutSnapshotsServerPriceAndThirtyPercentDepositAndIsIdempotent() throws Exception {
        UUID seller=account(),buyer=account(),product=product(seller,1001),request=UUID.randomUUID();
        String body=checkout(buyer,product,request); String repeated=checkout(buyer,product,request);
        assertThat(json.readTree(body)).isEqualTo(json.readTree(repeated));
        var order=json.readTree(body).get(0);
        assertThat(order.get("depositVnd").asText()).isEqualTo("300");
        assertThat(order.get("balanceVnd").asText()).isEqualTo("701");
        assertThat(order.get("depositRateBps").asInt()).isEqualTo(3000);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM camerahub.orders WHERE checkout_batch_id=?",Integer.class,request)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT listing_status FROM camerahub.products WHERE id=?",String.class,product)).isEqualTo("RESERVED");
        assertThat(body).doesNotContain("PRIVATE-SERIAL","storage_key","password");
    }
    @Test void refusesDoubleSaleSelfPurchaseAndMissingCsrf() throws Exception {
        UUID seller=account(),buyer=account(),other=account(),product=product(seller,24500000);
        String payload=json.writeValueAsString(new MarketplaceController.Checkout(UUID.randomUUID(),List.of(product),"DELIVERY",address));
        mvc.perform(post("/api/marketplace/checkout").with(user(buyer.toString())).contentType("application/json").content(payload)).andExpect(status().isForbidden());
        mvc.perform(post("/api/marketplace/checkout").with(user(seller.toString())).with(csrf()).contentType("application/json").content(payload)).andExpect(status().isBadRequest());
        checkout(buyer,product,UUID.randomUUID());
        mvc.perform(post("/api/marketplace/checkout").with(user(other.toString())).with(csrf()).contentType("application/json")
            .content(json.writeValueAsString(new MarketplaceController.Checkout(UUID.randomUUID(),List.of(product),"DELIVERY",address)))).andExpect(status().isConflict());
    }
    @Test void enforcesPaymentStagesOwnershipAndRetrySafety() throws Exception {
        UUID seller=account(),buyer=account(),stranger=account(),product=product(seller,24500000);
        UUID order=UUID.fromString(json.readTree(checkout(buyer,product,UUID.randomUUID())).get(0).get("id").asText());
        String path="/api/marketplace/orders/"+order;
        mvc.perform(get(path).with(user(stranger.toString()))).andExpect(status().isNotFound());
        mvc.perform(post(path+"/pay").with(user(seller.toString())).with(csrf()).contentType("application/json").content("{\"stage\":\"DEPOSIT\"}")).andExpect(status().isNotFound());
        mvc.perform(post(path+"/pay").with(user(buyer.toString())).with(csrf()).contentType("application/json").content("{\"stage\":\"BALANCE_AND_SHIPPING\"}")).andExpect(status().isConflict());
        for(String stage:List.of("DEPOSIT","DEPOSIT","BALANCE_AND_SHIPPING","BALANCE_AND_SHIPPING"))
            mvc.perform(post(path+"/pay").with(user(buyer.toString())).with(csrf()).contentType("application/json").content("{\"stage\":\""+stage+"\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM camerahub.payment_records WHERE order_id=?",Integer.class,order)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT sum(amount_vnd) FROM camerahub.payment_records WHERE order_id=?",Long.class,order)).isEqualTo(24500000);
        assertThat(jdbc.queryForObject("SELECT order_status FROM camerahub.orders WHERE id=?",String.class,order)).isEqualTo("READY_TO_SHIP");
        mvc.perform(post(path+"/cancel").with(user(buyer.toString())).with(csrf()).contentType("application/json").content("{}" )).andExpect(status().isConflict());
    }
    @Test void unpaidCancellationReleasesOnlyItsReservedProduct() throws Exception {
        UUID seller=account(),buyer=account(),product=product(seller,24500000);
        UUID order=UUID.fromString(json.readTree(checkout(buyer,product,UUID.randomUUID())).get(0).get("id").asText());
        mvc.perform(post("/api/marketplace/orders/"+order+"/cancel").with(user(buyer.toString())).with(csrf())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT listing_status FROM camerahub.products WHERE id=?",String.class,product)).isEqualTo("PUBLISHED");
        checkout(account(),product,UUID.randomUUID());
    }
    @Test void publicDetailHidesDraftsAndPrivateSerialsAndListingsRequireVerification() throws Exception {
        UUID seller=account(),product=product(seller,24500000);
        mvc.perform(get("/api/public/products/"+product)).andExpect(status().isOk()).andExpect(jsonPath("$.priceVnd").value("24500000"));
        jdbc.update("UPDATE camerahub.products SET listing_status='DRAFT' WHERE id=?",product);
        mvc.perform(get("/api/public/products/"+product)).andExpect(status().isNotFound());
        UUID model=jdbc.queryForObject("SELECT model_id FROM camerahub.products WHERE id=?",UUID.class,product);
        var listing=new MarketplaceController.Listing(model,"Fixture listing","24500000",100,"Description","Condition","Accessories","PRIVATE",List.of("TRAVEL"));
        mvc.perform(post("/api/marketplace/listings").with(user(seller.toString())).with(csrf()).contentType("application/json").content(json.writeValueAsString(listing))).andExpect(status().isForbidden());
    }
    @Test void productPhotosAreValidatedAndPrivateUntilPublicationExceptOwnerAndAdmin() throws Exception {
        UUID seller=account(),admin=account(),product=product(seller,24500000);
        jdbc.update("INSERT INTO camerahub.seller_verifications(seller_id,status) VALUES (?,'VERIFIED')",seller);
        jdbc.update("INSERT INTO camerahub.user_roles(user_id,role_code) VALUES (?,'ADMIN')",admin);
        jdbc.update("UPDATE camerahub.products SET listing_status='PENDING_REVIEW' WHERE id=?",product);
        var bytes=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
        String body=mvc.perform(multipart("/api/marketplace/listings/"+product+"/media")
            .file(new MockMultipartFile("file","camera.png","image/png",bytes.toByteArray()))
            .with(user(seller.toString())).with(csrf())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String url=json.readTree(body).get("url").asText();
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(get(url).with(user(seller.toString()))).andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(get(url).with(user(admin.toString()).roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(multipart("/api/marketplace/listings/"+product+"/media")
            .file(new MockMultipartFile("file","fake.png","image/png","not-an-image".getBytes()))
            .with(user(seller.toString())).with(csrf())).andExpect(status().isBadRequest());
        jdbc.update("UPDATE camerahub.products SET listing_status='PUBLISHED' WHERE id=?",product);
        mvc.perform(get(url)).andExpect(status().isOk());
    }
    @Test void brandOnlyListingUsesUnspecifiedModelAndPreservesExistingModels() throws Exception {
        UUID seller=account(),oldProduct=product(seller,24500000);
        jdbc.update("INSERT INTO camerahub.seller_verifications(seller_id,status) VALUES (?,'VERIFIED')",seller);
        UUID brand=jdbc.queryForObject("SELECT m.brand_id FROM camerahub.products p JOIN camerahub.camera_models m ON m.id=p.model_id WHERE p.id=?",UUID.class,oldProduct);
        UUID originalModel=jdbc.queryForObject("SELECT model_id FROM camerahub.products WHERE id=?",UUID.class,oldProduct);
        var input=new MarketplaceController.Listing(null,"Canon do người bán đặt tiêu đề","12000000",null,"Description","Condition","Pin","PRIVATE",List.of("TRAVEL"),brand,"BODY");
        for(int i=0;i<2;i++) {
            String body=mvc.perform(post("/api/marketplace/listings").with(user(seller.toString())).with(csrf())
                .contentType("application/json").content(json.writeValueAsString(input))).andExpect(status().isOk())
                .andExpect(jsonPath("$.listingStatus").value("PENDING_REVIEW")).andReturn().getResponse().getContentAsString();
            UUID id=UUID.fromString(json.readTree(body).get("id").asText());
            assertThat(jdbc.queryForObject("SELECT m.model_name FROM camerahub.products p JOIN camerahub.camera_models m ON m.id=p.model_id WHERE p.id=?",String.class,id)).isEqualTo("Chưa khai báo model");
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM camerahub.camera_models WHERE brand_id=? AND model_name='Chưa khai báo model' AND device_type='BODY'",Integer.class,brand)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT model_id FROM camerahub.products WHERE id=?",UUID.class,oldProduct)).isEqualTo(originalModel);
        mvc.perform(get("/api/public/brands")).andExpect(status().isOk());
        // The previous modelId request remains accepted.
        var legacy=new MarketplaceController.Listing(originalModel,"Legacy listing","12000000",null,"Description","Condition","Pin","PRIVATE",List.of());
        mvc.perform(post("/api/marketplace/listings").with(user(seller.toString())).with(csrf()).contentType("application/json")
            .content(json.writeValueAsString(legacy))).andExpect(status().isOk());
    }
    @Test void brandListingRejectsMissingUnknownOrAmbiguousSelection() throws Exception {
        UUID seller=account(); jdbc.update("INSERT INTO camerahub.seller_verifications(seller_id,status) VALUES (?,'VERIFIED')",seller);
        String base="\"title\":\"Fixture\",\"priceVnd\":\"12000000\",\"description\":\"Description\",\"conditionNotes\":\"Condition\",\"purposes\":[]";
        for(String selection:List.of("",",\"brandId\":\""+UUID.randomUUID()+"\"",",\"brandId\":\""+UUID.randomUUID()+"\",\"modelId\":\""+UUID.randomUUID()+"\"",",\"brandId\":\""+UUID.randomUUID()+"\",\"deviceType\":\"INVALID\"")) {
            mvc.perform(post("/api/marketplace/listings").with(user(seller.toString())).with(csrf())
                .contentType("application/json").content("{"+base+selection+"}")).andExpect(status().isBadRequest());
        }
    }
}
