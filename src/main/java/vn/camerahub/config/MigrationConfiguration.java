package vn.camerahub.config;

import java.sql.Connection;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MigrationConfiguration {
    @Bean
    FlywayMigrationStrategy guardedMigration(DataSource dataSource) {
        return flyway -> {
            // Session locks require a direct Neon endpoint, not its transaction pooler.
            try (Connection connection = dataSource.getConnection();
                 Statement lock = connection.createStatement()) {
                lock.execute("SET lock_timeout = '30s'");
                lock.execute("SELECT pg_advisory_lock(1128353106, 0)");
                try {
                    DatabaseSafety.verify(connection);
                    flyway.migrate();
                    flyway.validate();
                } finally {
                    lock.execute("SELECT pg_advisory_unlock(1128353106, 0)");
                }
            } catch (java.sql.SQLException exception) {
                throw new IllegalStateException("DATABASE_PREFLIGHT_FAILED: Check local database configuration.", exception);
            }
        };
    }
}
