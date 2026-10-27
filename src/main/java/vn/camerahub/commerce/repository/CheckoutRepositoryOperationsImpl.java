package vn.camerahub.commerce.repository;

import java.util.*;

import vn.camerahub.shared.persistence.*;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
@lombok.RequiredArgsConstructor
public class CheckoutRepositoryOperationsImpl implements CheckoutRepositoryOperations {
    private final JpaQueries queries;

    public List<Map<String, Object>> findShowrooms(Object... args) {
        return queries.queryForList("SELECT id,name,address FROM camerahub.showrooms WHERE active ORDER BY name", args);
    }

    public int insertCheckoutBatches(Object... args) {
        return queries.update("INSERT INTO camerahub.checkout_batches(id,buyer_id,request_hash,request) VALUES (?,?,?,?::jsonb) ON CONFLICT DO NOTHING", args);
    }

    public Map<String, Object> findCheckoutBatches(Object... args) {
        return queries.queryForMap("SELECT buyer_id,request_hash,state FROM camerahub.checkout_batches WHERE id=?", args);
    }

    public <T> T findCheckoutBatches2(Class<T> type, Object... args) {
        return queries.queryForObject("SELECT state FROM camerahub.checkout_batches WHERE id=?", type, args);
    }

    public <T> List<T> findOrders(Class<T> type, Object... args) {
        return queries.queryForList("SELECT id FROM camerahub.orders WHERE checkout_batch_id=? ORDER BY id", type, args);
    }

    public List<Map<String, Object>> findCheckoutBatches3(Object... args) {
        return queries.queryForList("UPDATE camerahub.checkout_batches SET lease_token=?,leased_until=now()+interval '60 seconds' WHERE id=? AND state='PLACING' AND (leased_until IS NULL OR leased_until<now()) RETURNING *", args);
    }

    public Map<String, Object> findCheckoutBatches4(Object... args) {
        return queries.queryForMap("SELECT state,lease_token FROM camerahub.checkout_batches WHERE id=? FOR UPDATE", args);
    }

    public int insertOrders(Object... args) {
        return queries.update("""
                INSERT INTO camerahub.orders(id,product_id,buyer_id,seller_id,agreed_price_vnd,product_snapshot,shipping_address_snapshot,payment_deadline,deposit_deadline,checkout_batch_id)
                VALUES (?,?,?,?,?,?::jsonb,?::jsonb,now()+interval '24 hours',now()+interval '24 hours',?)
                """, args);
    }

    public int insertOrderDeposits(Object... args) {
        return queries.update("INSERT INTO camerahub.order_deposits(order_id,amount_vnd,deposit_rate_bps) SELECT id,deposit_amount_vnd,deposit_rate_bps FROM camerahub.orders WHERE id=?", args);
    }

    public int updateCheckoutBatches(Object... args) {
        return queries.update("UPDATE camerahub.checkout_batches SET state='PLACED',reservations=?::jsonb,updated_at=now(),leased_until=NULL WHERE id=?", args);
    }

    public int updateCheckoutBatches2(Object... args) {
        return queries.update("UPDATE camerahub.checkout_batches SET state='FAILED',last_error=?,leased_until=NULL,updated_at=now() WHERE id=? AND lease_token=? AND state='PLACING'", args);
    }

    public int updateCheckoutBatches3(Object... args) {
        return queries.update("UPDATE camerahub.checkout_batches SET leased_until=NULL WHERE id=? AND lease_token=?", args);
    }

    public int updateCheckoutBatches4(Object... args) {
        return queries.update("UPDATE camerahub.checkout_batches SET leased_until=NULL WHERE id=? AND lease_token=?", args);
    }

    public List<Map<String, Object>> findShowrooms2(Object... args) {
        return queries.queryForList("SELECT name,address FROM camerahub.showrooms WHERE id=? AND active", args);
    }

    public List<Map<String, Object>> findOrders2(Object... args) {
        return queries.queryForList("SELECT order_status FROM camerahub.orders WHERE id=? AND buyer_id=? FOR UPDATE", args);
    }

    public int updateOrders(Object... args) {
        return queries.update("UPDATE camerahub.orders SET order_status='CANCELLED',updated_at=now() WHERE id=?", args);
    }

    public int updateOrderDeposits(Object... args) {
        return queries.update("UPDATE camerahub.order_deposits SET status='CANCELLED' WHERE order_id=?", args);
    }

    public int updateCheckoutBatches5(Object... args) {
        return queries.update("UPDATE camerahub.checkout_batches SET reservation_synced=true WHERE id=?", args);
    }

    public Map<String, Object> findOrders3(Object... args) {
        return queries.queryForMap("SELECT product_id,checkout_batch_id FROM camerahub.orders WHERE id=?", args);
    }

    public int updateOrders2(Object... args) {
        return queries.update("UPDATE camerahub.orders SET release_synced=true WHERE id=?", args);
    }

    public <T> List<T> findCheckoutBatches5(Class<T> type, Object... args) {
        return queries.queryForList("SELECT id FROM camerahub.checkout_batches WHERE state='PLACING' AND (leased_until IS NULL OR leased_until<now()) ORDER BY created_at LIMIT 5", type, args);
    }

    public <T> List<T> findCheckoutBatches6(Class<T> type, Object... args) {
        return queries.queryForList("SELECT id FROM camerahub.checkout_batches WHERE state='PLACED' AND NOT reservation_synced LIMIT 5", type, args);
    }

    public <T> List<T> findOrders4(Class<T> type, Object... args) {
        return queries.queryForList("SELECT id FROM camerahub.orders WHERE order_status='CANCELLED' AND NOT release_synced AND checkout_batch_id IS NOT NULL LIMIT 5", type, args);
    }

    public <T> List<T> findCheckoutBatches7(Class<T> type, Object... args) {
        return queries.queryForList("SELECT id FROM camerahub.checkout_batches WHERE state='FAILED' AND NOT reservation_synced LIMIT 5", type, args);
    }

    public int updateCheckoutBatches6(Object... args) {
        return queries.update("UPDATE camerahub.checkout_batches SET reservation_synced=true WHERE id=?", args);
    }
}

