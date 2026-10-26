import java.sql.*;
import java.util.*;

/**
 * One-time, read-only source snapshot. Destination transactions never overwrite existing rows.
 */
public class ImportLegacy {
    static String env(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + key);
        return value;
    }

    static Connection target(String service, int port) throws SQLException {
        return DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + port + "/camerahub_" + service, "camerahub_" + service, env(service.toUpperCase() + "_DB_PASSWORD"));
    }

    static LinkedHashMap<String, String> columns(Connection db, String table) throws SQLException {
        var result = new LinkedHashMap<String, String>();
        try (var q = db.prepareStatement("SELECT column_name,data_type FROM information_schema.columns WHERE table_schema='camerahub' AND table_name=? AND is_generated='NEVER' ORDER BY ordinal_position")) {
            q.setString(1, table);
            try (var r = q.executeQuery()) {
                while (r.next()) result.put(r.getString(1), r.getString(2));
            }
        }
        return result;
    }

    static int copy(Connection source, Connection destination, String table) throws SQLException {
        var sourceColumns = columns(source, table);
        var destColumns = columns(destination, table);
        destColumns.keySet().retainAll(sourceColumns.keySet());
        if (destColumns.isEmpty()) return 0;
        var fields = new ArrayList<>(destColumns.keySet());
        String names = String.join(",", fields);
        String placeholders = String.join(",", Collections.nCopies(fields.size(), "?"));
        int count = 0;
        String supportedRoles = "user_roles".equals(table) ? " WHERE role_code IN ('BUYER','SELLER','ADMIN')" : "";
        try (var select = source.prepareStatement("SELECT " + names + " FROM camerahub." + table + supportedRoles); var insert = destination.prepareStatement("INSERT INTO camerahub." + table + "(" + names + ") VALUES (" + placeholders + ") ON CONFLICT DO NOTHING")) {
            select.setFetchSize(200);
            try (var rows = select.executeQuery()) {
                while (rows.next()) {
                    for (int i = 0; i < fields.size(); i++) insert.setObject(i + 1, rows.getObject(i + 1));
                    count += insert.executeUpdate();
                }
            }
        }
        System.out.println(table + ": " + count + " imported");
        return count;
    }

    static void queryCopy(Connection source, Connection destination, String select, String insert) throws SQLException {
        try (var q = source.createStatement(); var rows = q.executeQuery(select); var statement = destination.prepareStatement(insert)) {
            while (rows.next()) {
                for (int n = 1; n <= rows.getMetaData().getColumnCount(); n++)
                    statement.setObject(n, rows.getObject(n));
                statement.executeUpdate();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        try (var source = DriverManager.getConnection(env("DB_URL"), env("DB_USERNAME"), env("DB_PASSWORD")); var identity = target("identity", 15431); var market = target("marketplace", 15432); var commerce = target("commerce", 15434)) {
            source.setReadOnly(true);
            source.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            source.setAutoCommit(false);
            for (var db : List.of(identity, market, commerce)) db.setAutoCommit(false);
            try {
                for (String table : List.of("users", "user_roles", "user_addresses", "auth_identities", "audit_logs"))
                    copy(source, identity, table);
                // Historical inspector accounts remain ordinary users; never import a retired permission.
                queryCopy(source, identity,
                        "SELECT DISTINCT user_id,'BUYER' FROM camerahub.user_roles WHERE role_code='INSPECTOR'",
                        "INSERT INTO camerahub.user_roles(user_id,role_code) VALUES (?,?) ON CONFLICT DO NOTHING");
                queryCopy(source, market, "SELECT id,full_name,account_status,created_at FROM camerahub.users", "INSERT INTO camerahub.public_accounts(id,full_name,account_status,created_at) VALUES (?,?,?,?) ON CONFLICT DO NOTHING");
                for (String table : List.of("brands", "camera_models", "products", "product_media", "product_status_history"))
                    copy(source, market, table);
                // Preserve historical inspection facts in Marketplace's own product read model.
                queryCopy(source, market, """
                        SELECT COALESCE(jsonb_agg(jsonb_build_object('name',definition.display_name,'result',result.result,'notes',result.notes) ORDER BY definition.code),'[]'::jsonb),request.product_id
                        FROM camerahub.inspection_requests request JOIN camerahub.inspection_reports report ON report.request_id=request.id
                        JOIN camerahub.inspection_results result ON result.report_id=report.id JOIN camerahub.inspection_check_definitions definition ON definition.code=result.check_code
                        WHERE report.superseded_at IS NULL AND report.id=(SELECT latest.id FROM camerahub.inspection_reports latest JOIN camerahub.inspection_requests q ON q.id=latest.request_id WHERE q.product_id=request.product_id AND latest.superseded_at IS NULL ORDER BY latest.issued_at DESC LIMIT 1)
                        GROUP BY request.product_id
                        """, "UPDATE camerahub.products SET inspection_checks=?::jsonb WHERE id=?");
                queryCopy(source, commerce, "SELECT id,buyer_id,request_hash FROM camerahub.checkout_batches", "INSERT INTO camerahub.checkout_batches(id,buyer_id,request_hash,state,request,reservation_synced) VALUES (?,?,?,'PLACED','{}'::jsonb,true) ON CONFLICT DO NOTHING");
                // An order predating batch checkout gets its own stable saga ID.
                queryCopy(source, commerce, "SELECT id,buyer_id FROM camerahub.orders", "INSERT INTO camerahub.checkout_batches(id,buyer_id,request_hash,state,request,reservation_synced) VALUES (?,?,'legacy','PLACED','{}'::jsonb,true) ON CONFLICT DO NOTHING");
                for (String table : List.of("orders", "payment_records", "order_deposits", "showrooms", "buyback_requests", "seller_reviews"))
                    copy(source, commerce, table);
                try (var s = commerce.createStatement()) {
                    s.executeUpdate("UPDATE camerahub.orders SET checkout_batch_id=id WHERE checkout_batch_id IS NULL");
                }
                // Reconnect legacy reserved inventory so cancellation is still idempotent after the cutover.
                queryCopy(source, market, "SELECT COALESCE(checkout_batch_id,id),buyer_id,jsonb_agg(product_id ORDER BY product_id) FROM camerahub.orders WHERE order_status<>'CANCELLED' GROUP BY COALESCE(checkout_batch_id,id),buyer_id", "INSERT INTO camerahub.reservation_batches(id,buyer_id,product_ids,state) VALUES (?,?,?::jsonb,'CONFIRMED') ON CONFLICT DO NOTHING");
                queryCopy(source, market, "SELECT product_id,COALESCE(checkout_batch_id,id),buyer_id,gen_random_uuid(),product_snapshot FROM camerahub.orders WHERE order_status<>'CANCELLED'", "INSERT INTO camerahub.product_reservations(product_id,batch_id,buyer_id,token,snapshot,state) VALUES (?,?,?,?,?::jsonb,'CONFIRMED') ON CONFLICT DO NOTHING");
                identity.commit();
                market.commit();
                commerce.commit();
                source.rollback();
                System.out.println("Import complete. Original database was read-only. Rebuild Moderation index before enabling the gateway.");
            } catch (Exception e) {
                identity.rollback();
                market.rollback();
                commerce.rollback();
                source.rollback();
                throw e;
            }
        }
    }
}
