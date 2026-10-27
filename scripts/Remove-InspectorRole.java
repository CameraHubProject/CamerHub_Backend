import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Explicit, transactional role cleanup for the legacy Neon DB and local Identity DB. */
class RemoveInspectorRole {
    private static final Path MIGRATION = Path.of("src/main/resources/db/unified/V3__remove_inspector_role.sql");

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !Set.of("check", "test", "apply").contains(args[0]) || !Set.of("local", "neon").contains(args[1]))
            throw new IllegalArgumentException("Usage: Remove-InspectorRole.java check|test|apply local|neon");
        try (var db = connect(args[1])) {
            if ("test".equals(args[0])) test(db);
            else if ("apply".equals(args[0])) apply(db, args[1]);
            else {
                db.setReadOnly(true);
                System.out.println(args[1] + ": retired role definitions=" + count(db, "SELECT count(*) FROM camerahub.roles WHERE code='INSPECTOR'")
                        + ", retired assignments=" + count(db, "SELECT count(*) FROM camerahub.user_roles WHERE role_code='INSPECTOR'"));
            }
        } catch (SQLException error) {
            // JDBC messages can contain URLs; report only a safe diagnostic.
            Throwable cause = error;
            while (cause.getCause() != null) cause = cause.getCause();
            System.err.println("Role cleanup failed; target=" + args[1] + ", SQLSTATE=" + error.getSQLState() + ", cause=" + cause.getClass().getSimpleName());
            System.exit(1);
        }
    }

    private static Map<String, String> settings() throws Exception {
        var values = new HashMap<String, String>();
        for (String file : List.of(".env", ".local/microservices.env")) {
            Path path = Path.of(file);
            if (!Files.exists(path)) continue;
            for (String line : Files.readAllLines(path)) {
                String text = line.strip();
                if (text.isEmpty() || text.startsWith("#") || !text.contains("=")) continue;
                int separator = text.indexOf('=');
                String value = text.substring(separator + 1);
                if (value.length() > 1 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))))
                    value = value.substring(1, value.length() - 1);
                values.put(text.substring(0, separator), value);
            }
        }
        return values;
    }

    private static Connection connect(String target) throws Exception {
        Class.forName("org.postgresql.Driver");
        var settings = settings();
        String url = "neon".equals(target) ? settings.get("DB_URL") : "jdbc:postgresql://127.0.0.1:15431/camerahub_identity";
        var properties = new Properties();
        properties.setProperty("user", "neon".equals(target) ? settings.get("DB_USERNAME") : "camerahub_identity");
        properties.setProperty("password", "neon".equals(target) ? settings.get("DB_PASSWORD") : settings.get("IDENTITY_DB_PASSWORD"));
        properties.setProperty("connectTimeout", "12");
        properties.setProperty("socketTimeout", "30");
        DriverManager.setLoginTimeout(12);
        return DriverManager.getConnection(url, properties);
    }

    private static void execute(Connection db, String sql) throws SQLException {
        try (var statement = db.createStatement()) { statement.execute(sql); }
    }

    private static long count(Connection db, String sql) throws SQLException {
        try (var statement = db.createStatement(); var rows = statement.executeQuery(sql)) { rows.next(); return rows.getLong(1); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void apply(Connection db, String target) throws Exception {
        db.setAutoCommit(false);
        try {
            execute(db, "SET LOCAL lock_timeout='5s'; SET LOCAL statement_timeout='20s'; LOCK TABLE camerahub.roles, camerahub.user_roles IN SHARE ROW EXCLUSIVE MODE");
            long users = count(db, "SELECT count(*) FROM camerahub.users");
            long admins = count(db, "SELECT count(*) FROM camerahub.user_roles WHERE role_code='ADMIN'");
            long assignments = count(db, "SELECT count(*) FROM camerahub.user_roles WHERE role_code='INSPECTOR'");
            var backup = new ArrayList<String>();
            backup.add("user_id,role_code");
            try (var statement = db.createStatement(); var rows = statement.executeQuery("SELECT user_id,role_code FROM camerahub.user_roles WHERE user_id IN (SELECT user_id FROM camerahub.user_roles WHERE role_code='INSPECTOR') ORDER BY user_id,role_code")) {
                while (rows.next()) backup.add(rows.getString(1) + "," + rows.getString(2));
            }
            Path directory = Path.of(".local/role-removal");
            Files.createDirectories(directory);
            Path file = directory.resolve(target + "-" + Instant.now().toEpochMilli() + "-roles.csv");
            Files.write(file, backup, StandardOpenOption.CREATE_NEW);
            execute(db, Files.readString(MIGRATION));
            require(count(db, "SELECT count(*) FROM camerahub.user_roles WHERE role_code='INSPECTOR'") == 0, "Retired assignments remain");
            require(count(db, "SELECT count(*) FROM camerahub.roles WHERE code='INSPECTOR'") == 0, "Retired role remains");
            require(count(db, "SELECT count(*) FROM camerahub.user_roles WHERE role_code='ADMIN'") == admins, "Admin permissions changed unexpectedly");
            require(count(db, "SELECT count(*) FROM camerahub.users") >= users, "Accounts were removed");
            var savepoint = db.setSavepoint();
            boolean blocked = false;
            try { execute(db, "INSERT INTO camerahub.roles(code) VALUES ('INSPECTOR')"); }
            catch (SQLException error) { if (!"23514".equals(error.getSQLState())) throw error; blocked = true; }
            db.rollback(savepoint);
            require(blocked, "Database still accepts the retired role");
            db.commit();
            System.out.println("APPLIED " + target + ": removed assignments=" + assignments + "; accounts and Admin permissions preserved; backup=" + file);
        } catch (Exception error) {
            db.rollback();
            throw error;
        }
    }

    private static void test(Connection db) throws Exception {
        String schema = "role_test_" + UUID.randomUUID().toString().replace("-", "");
        db.setAutoCommit(false);
        try {
            execute(db, "CREATE SCHEMA " + schema + "; SET LOCAL search_path TO " + schema);
            execute(db, "CREATE TABLE users(id integer PRIMARY KEY); CREATE TABLE roles(code varchar(20) PRIMARY KEY CHECK(code IN ('BUYER','SELLER','ADMIN','INSPECTOR')));"
                    + " CREATE TABLE user_roles(user_id integer REFERENCES users(id),role_code varchar(20) REFERENCES roles(code),PRIMARY KEY(user_id,role_code));"
                    + " CREATE TABLE inspection_reports(id integer PRIMARY KEY,inspector_id integer REFERENCES users(id));"
                    + " INSERT INTO users VALUES (1),(2),(3),(4),(5); INSERT INTO roles VALUES ('BUYER'),('SELLER'),('ADMIN'),('INSPECTOR');"
                    + " INSERT INTO user_roles VALUES (1,'INSPECTOR'),(2,'BUYER'),(2,'INSPECTOR'),(3,'ADMIN'),(3,'INSPECTOR'),(4,'SELLER'),(5,'BUYER'),(5,'ADMIN');"
                    + " INSERT INTO inspection_reports VALUES (1,1)");
            String sql = Files.readString(MIGRATION).replace("camerahub.", schema + ".");
            execute(db, sql);
            require(count(db, "SELECT count(*) FROM users") == 5, "Accounts lost");
            require(count(db, "SELECT count(*) FROM inspection_reports WHERE inspector_id=1") == 1, "Historical report lost");
            require(count(db, "SELECT count(*) FROM user_roles WHERE user_id IN (1,2) AND role_code='BUYER'") == 2, "Ordinary access not preserved");
            require(count(db, "SELECT count(*) FROM user_roles WHERE user_id=3 AND role_code='ADMIN'") == 1, "Existing Admin lost");
            require(count(db, "SELECT count(*) FROM user_roles WHERE user_id IN (1,2,4) AND role_code='ADMIN'") == 0, "Unexpected privilege escalation");
            require(count(db, "SELECT count(*) FROM user_roles WHERE user_id=4 AND role_code='SELLER'") == 1, "Unrelated role changed");
            require(count(db, "SELECT count(*) FROM roles WHERE code='INSPECTOR'") == 0, "Retired role remains");
            long remaining = count(db, "SELECT count(*) FROM user_roles");
            execute(db, sql);
            require(count(db, "SELECT count(*) FROM user_roles") == remaining, "Migration is not idempotent");
            System.out.println("PASS 8 PostgreSQL migration checks; test schema and all fixtures rolled back.");
        } finally {
            db.rollback();
        }
    }
}
