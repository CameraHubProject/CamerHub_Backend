import java.sql.*;
import java.util.*;

/**
 * Compares only legacy-owned rows/columns; ignores new platform smoke fixtures and derived fields.
 */
public class AuditLegacy {
    static String env(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + name);
        return value;
    }

    static Set<String> columns(Connection db, String table) throws SQLException {
        var result = new LinkedHashSet<String>();
        try (var q = db.prepareStatement("SELECT column_name FROM information_schema.columns WHERE table_schema='camerahub' AND table_name=? AND is_generated='NEVER' ORDER BY ordinal_position")) {
            q.setString(1, table);
            try (var rows = q.executeQuery()) {
                while (rows.next()) result.add(rows.getString(1));
            }
        }
        return result;
    }

    static int compare(Connection source, Connection target, String table) throws SQLException {
        var fields = columns(source, table);
        fields.retainAll(columns(target, table));
        fields.removeAll(Set.of("checkout_batch_id", "image_sha256", "image_dhash", "image_signature"));
        var keys = switch (table) {
            case "user_roles" -> List.of("user_id", "role_code");
            case "auth_identities" -> List.of("provider", "subject");
            default -> List.of("id");
        };
        String names = String.join(",", fields), where = String.join(" AND ", keys.stream().map(k -> k + "=?").toList());
        int count = 0, mismatch = 0;
        try (var read = source.createStatement(); var rows = read.executeQuery("SELECT " + names + " FROM camerahub." + table); var lookup = target.prepareStatement("SELECT " + names + " FROM camerahub." + table + " WHERE " + where)) {
            while (rows.next()) {
                count++;
                for (int i = 0; i < keys.size(); i++) lookup.setObject(i + 1, rows.getObject(keys.get(i)));
                try (var copied = lookup.executeQuery()) {
                    if (!copied.next()) {
                        mismatch++;
                        continue;
                    }
                    for (String field : fields) {
                        Object a = rows.getObject(field), b = copied.getObject(field);
                        if (!Objects.equals(a, b) && !Objects.equals(Objects.toString(a, null), Objects.toString(b, null))) {
                            mismatch++;
                            break;
                        }
                    }
                }
            }
        }
        System.out.println(table + ": checked=" + count + ", mismatches=" + mismatch);
        return mismatch;
    }

    public static void main(String[] args) throws Exception {
        int errors = 0;
        try (var source = DriverManager.getConnection(env("DB_URL"), env("DB_USERNAME"), env("DB_PASSWORD"))) {
            source.setReadOnly(true);
            source.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            source.setAutoCommit(false);
            var groups = Map.of("identity", List.of("users", "user_roles", "user_addresses", "auth_identities", "audit_logs"), "marketplace", List.of("brands", "camera_models", "products", "product_media", "product_status_history"), "commerce", List.of("orders", "payment_records", "order_deposits", "showrooms", "buyback_requests", "seller_reviews"));
            var ports = Map.of("identity", 15431, "marketplace", 15432, "commerce", 15434);
            for (var group : groups.entrySet())
                try (var target = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + ports.get(group.getKey()) + "/camerahub_" + group.getKey(), "camerahub_" + group.getKey(), env(group.getKey().toUpperCase() + "_DB_PASSWORD"))) {
                    target.setReadOnly(true);
                    for (String table : group.getValue()) errors += compare(source, target, table);
                }
            source.rollback();
        }
        if (errors != 0)
            throw new IllegalStateException("Source changed or import incomplete: " + errors + " mismatched rows. Resolve before cutover.");
        System.out.println("All original supported rows match the new service databases.");
    }
}
