import java.sql.DriverManager;

/** Read-only metadata inspection; never prints credentials or application rows. */
class InspectDatabase {
    public static void main(String[] args) throws Exception {
        try (var connection = DriverManager.getConnection(System.getenv("DB_URL"),
                System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"))) {
            connection.setReadOnly(true);
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                try (var rows = statement.executeQuery("SHOW server_version")) {
                    rows.next();
                    System.out.println("PostgreSQL: " + rows.getString(1));
                }
                try (var rows = statement.executeQuery("""
                        SELECT table_schema, count(*) FROM information_schema.tables
                        WHERE table_schema <> 'information_schema' AND table_schema !~ '^pg_'
                        GROUP BY table_schema ORDER BY table_schema
                        """)) {
                    while (rows.next()) System.out.println("Tables: " + rows.getString(1) + " = " + rows.getInt(2));
                }
                try (var rows = statement.executeQuery("""
                        SELECT n.nspname, c.relname, c.relkind::text FROM pg_class c
                        JOIN pg_namespace n ON n.oid = c.relnamespace
                        WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'
                        AND NOT EXISTS (SELECT 1 FROM pg_depend d
                          WHERE d.classid = 'pg_class'::regclass AND d.objid = c.oid AND d.deptype = 'e')
                        ORDER BY 1, 2 LIMIT 60
                        """)) {
                    while (rows.next()) System.out.println("Relation: " + rows.getString(1) + "." + rows.getString(2) + " (" + rows.getString(3) + ")");
                }
                try (var rows = statement.executeQuery("""
                        SELECT n.nspname, p.proname FROM pg_proc p
                        JOIN pg_namespace n ON n.oid = p.pronamespace
                        WHERE n.nspname <> 'information_schema' AND n.nspname !~ '^pg_'
                        AND NOT EXISTS (SELECT 1 FROM pg_depend d
                          WHERE d.classid = 'pg_proc'::regclass AND d.objid = p.oid AND d.deptype = 'e')
                        ORDER BY 1, 2 LIMIT 20
                        """)) {
                    while (rows.next()) System.out.println("Routine: " + rows.getString(1) + "." + rows.getString(2));
                }
            }
            connection.rollback();
        }
    }
}
