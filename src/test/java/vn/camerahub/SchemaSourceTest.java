package vn.camerahub;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SchemaSourceTest {
    @Test
    void migrationIsByteForByteIdenticalToAuthoritativeSql() throws Exception {
        byte[] source = Files.readAllBytes(Path.of(System.getProperty("project.root"), "db", "001_full_schema.sql"));
        try (var migration = getClass().getResourceAsStream("/db/migration/V1__full_schema.sql")) {
            assertThat(migration).isNotNull();
            assertThat(migration.readAllBytes()).isEqualTo(source);
        }
    }
}
