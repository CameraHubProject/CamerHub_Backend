package vn.camerahub.gateway;

import java.nio.file.*;
import java.util.*;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ArchitectureBoundaryTest {
    @Test
    void businessServicesUseJpaLayersAndDoNotImportAnotherServicesCode() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(root.resolve("microservices")) && root.getParent() != null) root = root.getParent();
        for (String module : List.of("identity", "marketplace", "moderation", "commerce", "ai-advisor")) {
            String domain = module.equals("ai-advisor") ? "ai" : module;
            Path source = root.resolve("microservices/" + module + "-service/src/main/java/vn/camerahub/" + domain);
            assertThat(Files.isDirectory(source.resolve("entity"))).isTrue();
            assertThat(Files.isDirectory(source.resolve("repository"))).isTrue();
            assertThat(Files.isDirectory(source.resolve("service"))).isTrue();
            assertThat(Files.isDirectory(source.resolve("controller"))).isTrue();
            assertThat(Files.isDirectory(source.resolve("dto"))).isTrue();
            assertThat(Files.isDirectory(source.resolve("mapper"))).isTrue();
            try (var files = Files.walk(source)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String text = Files.readString(file), normalized = file.toString().replace('\\', '/');
                    if (!normalized.contains("/repository/"))
                        assertThat(text).as(file.toString()).doesNotContain("JdbcTemplate", "java.sql.", "org.springframework.jdbc");
                    if (normalized.contains("/controller/"))
                        assertThat(text).as(file.toString()).doesNotContain(".repository.");
                    assertThat(text).as(file.toString()).doesNotContain("JdbcTemplate", "org.springframework.jdbc");
                    for (String other : List.of("identity", "marketplace", "moderation", "commerce", "ai"))
                        if (!domain.equals(other))
                            assertThat(text).as(file.toString()).doesNotContain("import vn.camerahub." + other + ".");
                }
            }
        }
    }
}
