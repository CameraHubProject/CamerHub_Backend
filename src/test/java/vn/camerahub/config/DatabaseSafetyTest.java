package vn.camerahub.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DatabaseSafetyTest {
    @Test
    void acceptsEmptyPostgres16() throws Exception {
        assertThatCode(() -> DatabaseSafety.verify(database(160000, false, false, false)))
                .doesNotThrowAnyException();
    }

    @Test
    void refusesNonEmptyDatabaseBeforeFirstMigration() throws Exception {
        assertThatThrownBy(() -> DatabaseSafety.verify(database(160000, false, false, true)))
                .hasMessageContaining("DATABASE_NOT_EMPTY");
    }

    @Test
    void emptyHistoryDoesNotBypassNonEmptyDatabaseGuard() throws Exception {
        assertThatThrownBy(() -> DatabaseSafety.verify(database(160000, true, false, true)))
                .hasMessageContaining("DATABASE_NOT_EMPTY");
    }

    @Test
    void acceptsPreviouslyInstalledVersionForFlywayValidation() throws Exception {
        assertThatCode(() -> DatabaseSafety.verify(database(160000, true, true, true)))
                .doesNotThrowAnyException();
    }

    @Test
    void refusesUnsupportedPostgresVersion() throws Exception {
        assertThatThrownBy(() -> DatabaseSafety.verify(database(150000, false, false, false)))
                .hasMessageContaining("DATABASE_VERSION_UNSUPPORTED");
    }

    private Connection database(int version, boolean hasHistory, boolean hasV1, boolean occupied) throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);
        ResultSet server = mock(ResultSet.class);
        when(server.getInt(1)).thenReturn(version);
        ResultSet history = mock(ResultSet.class);
        when(history.getBoolean(1)).thenReturn(hasHistory);
        ResultSet migration = mock(ResultSet.class);
        when(migration.getBoolean(1)).thenReturn(hasV1);
        ResultSet objects = mock(ResultSet.class);
        when(objects.getBoolean(1)).thenReturn(occupied);
        if (hasHistory) {
            when(statement.executeQuery(anyString())).thenReturn(server, history, migration, objects);
        } else {
            when(statement.executeQuery(anyString())).thenReturn(server, history, objects);
        }
        return connection;
    }
}
