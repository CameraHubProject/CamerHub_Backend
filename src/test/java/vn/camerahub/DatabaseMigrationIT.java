package vn.camerahub;

import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Explicit opt-in: uses DB_URL and never creates, resets, or drops a database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DatabaseMigrationIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired TestRestTemplate http;
    @Autowired MockMvc mvc;

    @Test
    @Transactional
    void publicHomeFeedExcludesDraftsAndPrivateFields() throws Exception {
        UUID seller = UUID.randomUUID();
        UUID brand = UUID.randomUUID();
        UUID model = UUID.randomUUID();
        UUID published = UUID.randomUUID();
        jdbc.update("INSERT INTO camerahub.users(id,email,password_hash,full_name) VALUES (?,?,?,?)",
                seller, seller + "@example.invalid", "not-a-login-hash", "Integration fixture");
        jdbc.update("INSERT INTO camerahub.brands(id,name) VALUES (?,?)", brand, "Fixture-" + brand);
        jdbc.update("INSERT INTO camerahub.camera_models(id,brand_id,model_name,device_type) VALUES (?,?,?,'BODY')", model, brand, "Fixture camera");
        jdbc.update("""
                INSERT INTO camerahub.products(id,seller_id,model_id,title,price_vnd,description,
                  condition_notes,listing_status,serial_number_private)
                VALUES (?,?,?,'Public fixture',24500000,'Fixture','Used','PUBLISHED','PRIVATE-SERIAL-SENTINEL')
                """, published, seller, model);
        jdbc.update("""
                INSERT INTO camerahub.products(seller_id,model_id,title,price_vnd,description,condition_notes)
                VALUES (?,?,'UNPUBLISHED-SENTINEL',24500000,'Fixture','Used')
                """, seller, model);
        jdbc.update("INSERT INTO camerahub.product_media(product_id,storage_key,media_type) VALUES (?,'PRIVATE-KEY-SENTINEL','IMAGE')", published);
        String body = mvc.perform(get("/api/public/featured-products"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).contains("Public fixture", "\"priceVnd\":\"24500000\"", "NOT_INSPECTED")
                .doesNotContain("UNPUBLISHED-SENTINEL", "PRIVATE-SERIAL-SENTINEL", "PRIVATE-KEY-SENTINEL", "serial_number_private", "storage_key", "password_hash");
    }

    @Test
    void verifiesOriginalTablesAndPrototypeAdditions() {
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'camerahub' AND table_type = 'BASE TABLE'
                """, Integer.class)).isEqualTo(46);
        assertThat(jdbc.queryForList("SELECT code FROM camerahub.roles ORDER BY code", String.class))
                .containsExactly("ADMIN", "BUYER", "INSPECTOR", "SELLER");
        assertThat(jdbc.queryForList("SELECT code FROM camerahub.inspection_check_definitions ORDER BY code", String.class))
                .containsExactlyElementsOf(List.of("AUTOFOCUS", "BUTTONS", "EXTERIOR", "LCD_EVF", "PORTS", "SENSOR", "SERIAL_MATCH", "SHUTTER"));
        assertThat(jdbc.queryForObject("SELECT display_name FROM camerahub.shipping_providers WHERE code = 'MOCK'", String.class))
                .isEqualTo("CameraHub Mock Shipping");
    }

    @Test
    void repeatedStartupDoesNotReapplyV1() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        flyway.validate();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM public.flyway_schema_history WHERE success AND version = '1'", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void healthEndpointReachesDatabase() {
        var response = http.getForEntity("/api/health", String.class);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("\"status\":\"UP\"", "\"moneyMode\":\"SIMULATED\"");
    }
}
