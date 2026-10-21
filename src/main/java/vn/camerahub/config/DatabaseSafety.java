package vn.camerahub.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Preflight checks run before Flyway is allowed to write any metadata or DDL. */
final class DatabaseSafety {
    private DatabaseSafety() {}

    static void verify(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet version = statement.executeQuery("SHOW server_version_num")) {
                version.next();
                if (version.getInt(1) < 160000) {
                    throw new IllegalStateException("DATABASE_VERSION_UNSUPPORTED: PostgreSQL 16+ is required.");
                }
            }
            boolean installed;
            try (ResultSet history = statement.executeQuery("""
                    SELECT EXISTS (
                      SELECT 1 FROM information_schema.tables
                      WHERE table_schema = 'public' AND table_name = 'flyway_schema_history'
                    )
                    """)) {
                history.next();
                installed = history.getBoolean(1);
            }
            if (installed) {
                try (ResultSet history = statement.executeQuery("""
                        SELECT EXISTS (SELECT 1 FROM public.flyway_schema_history
                          WHERE version = '1' AND success = true
                            AND script = 'V1__full_schema.sql')
                        """)) {
                    history.next();
                    if (history.getBoolean(1)) {
                        return;
                    }
                }
            }
            try (ResultSet objects = statement.executeQuery("""
                    SELECT EXISTS (
                      SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
                      WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'
                        AND NOT (n.nspname = 'public' AND c.relname = 'flyway_schema_history')
                        AND NOT EXISTS (SELECT 1 FROM pg_depend d
                          WHERE d.classid = 'pg_class'::regclass AND d.objid = c.oid AND d.deptype = 'e')
                      UNION ALL
                      SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
                      WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'
                        AND NOT EXISTS (SELECT 1 FROM pg_depend d
                          WHERE d.classid = 'pg_proc'::regclass AND d.objid = p.oid AND d.deptype = 'e')
                      UNION ALL
                      SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid = t.typnamespace
                      WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'
                        AND t.typtype IN ('e', 'd', 'r', 'm')
                        AND NOT EXISTS (SELECT 1 FROM pg_depend d
                          WHERE d.classid = 'pg_type'::regclass AND d.objid = t.oid AND d.deptype = 'e')
                      UNION ALL
                      SELECT 1 FROM pg_namespace WHERE nspname = 'camerahub'
                    )
                    """)) {
                objects.next();
                if (objects.getBoolean(1)) {
                    throw new IllegalStateException(
                            "DATABASE_NOT_EMPTY: Refusing V1. Select a dedicated empty development database; no schema was changed.");
                }
            }
        }
    }
}
