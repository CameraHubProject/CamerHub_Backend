import java.sql.DriverManager;

/** Creates a separate development database; never resets or alters an existing one. */
class CreateDevelopmentDatabase {
    public static void main(String[] args) throws Exception {
        String name = "camerahub_mvp_dev";
        try (var connection = DriverManager.getConnection(System.getenv("DB_URL"),
                System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"))) {
            try (var check = connection.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
                check.setString(1, name);
                try (var rows = check.executeQuery()) {
                    if (rows.next()) {
                        System.out.println("Development database already exists; startup preflight must verify it before migration.");
                        return;
                    }
                }
            }
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE camerahub_mvp_dev TEMPLATE template0");
            }
            System.out.println("Created separate empty database: " + name);
        }
    }
}
