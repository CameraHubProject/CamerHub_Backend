import java.sql.*;
import java.util.*;

public class CleanupSmoke {
    static Connection db(String name, int port) throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + port + "/camerahub_" + name, "camerahub_" + name, System.getenv(name.toUpperCase() + "_DB_PASSWORD"));
    }

    public static void main(String[] args) throws Exception {
        var ids = new ArrayList<UUID>();
        try (var identity = db("identity", 15431); var market = db("marketplace", 15432); var moderation = db("moderation", 15433)) {
            try (var q = identity.createStatement(); var rows = q.executeQuery("SELECT id FROM camerahub.users WHERE email ~ '^micro-[0-9a-f]{32}@test[.]invalid$' AND full_name LIKE 'Microservices%'")) {
                while (rows.next()) ids.add(rows.getObject(1, UUID.class));
            }
            identity.setAutoCommit(false);
            market.setAutoCommit(false);
            moderation.setAutoCommit(false);
            for (UUID id : ids) {
                try (var q = identity.prepareStatement("UPDATE camerahub.users SET account_status='CLOSED' WHERE id=?")) {
                    q.setObject(1, id);
                    q.executeUpdate();
                }
                try (var q = identity.prepareStatement("DELETE FROM camerahub.user_roles WHERE user_id=? AND role_code='ADMIN'")) {
                    q.setObject(1, id);
                    q.executeUpdate();
                }
                try (var q = market.prepareStatement("UPDATE camerahub.products SET listing_status='HIDDEN' WHERE seller_id=? AND listing_status NOT IN ('RESERVED','SOLD')")) {
                    q.setObject(1, id);
                    q.executeUpdate();
                }
                try (var q = market.prepareStatement("UPDATE camerahub.public_accounts SET account_status='CLOSED' WHERE id=?")) {
                    q.setObject(1, id);
                    q.executeUpdate();
                }
                try (var q = moderation.prepareStatement("UPDATE camerahub.review_jobs SET state='SUPERSEDED' WHERE owner_id=?")) {
                    q.setObject(1, id);
                    q.executeUpdate();
                }
                try (var q = moderation.prepareStatement("DELETE FROM camerahub.listing_index WHERE snapshot->>'sellerId'=?")) {
                    q.setString(1, id.toString());
                    q.executeUpdate();
                }
            }
            identity.commit();
            market.commit();
            moderation.commit();
            System.out.println("Closed " + ids.size() + " smoke accounts and hid only their test listings.");
        }
    }
}
