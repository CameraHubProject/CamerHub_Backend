package vn.camerahub.marketplace.repository;

import java.util.*;

import vn.camerahub.marketplace.entity.Product;
import vn.camerahub.shared.persistence.*;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
@lombok.RequiredArgsConstructor
public class ReservationRepositoryOperationsImpl implements ReservationRepositoryOperations {
    private final JpaQueries queries;

    public List<Map<String, Object>> lockRequest(Object... args) {
        return queries.queryForList("SELECT pg_advisory_xact_lock(hashtext(?))", args);
    }

    public List<Map<String, Object>> lockBatch(Object... args) {
        return queries.queryForList("SELECT * FROM camerahub.reservation_batches WHERE id=? FOR UPDATE", args);
    }

    public int insertBatch(Object... args) {
        return queries.update("INSERT INTO camerahub.reservation_batches(id,buyer_id,product_ids) VALUES (?,?,?::jsonb)", args);
    }

    public int holdProduct(Object... args) {
        return queries.update("""
                INSERT INTO camerahub.product_reservations(product_id,batch_id,buyer_id,token,snapshot,state) VALUES (?,?,?,?,?::jsonb,'HELD')
                ON CONFLICT(product_id) DO UPDATE SET batch_id=EXCLUDED.batch_id,buyer_id=EXCLUDED.buyer_id,token=EXCLUDED.token,snapshot=EXCLUDED.snapshot,state='HELD',created_at=now()
                WHERE camerahub.product_reservations.state='RELEASED'
                """, args);
    }

    public int markReserved(Object... args) {
        return queries.update("UPDATE camerahub.products SET listing_status='RESERVED',updated_at=now(),version=version+1 WHERE id=?", args);
    }

    public int insertProductStatusHistory(Object... args) {
        return queries.update("INSERT INTO camerahub.product_status_history(product_id,old_status,new_status,actor_id,reason) VALUES (?,'PUBLISHED','RESERVED',?,'Giữ chỗ cho checkout')", args);
    }

    public int confirmReservations(Object... args) {
        return queries.update("UPDATE camerahub.product_reservations SET state='CONFIRMED' WHERE batch_id=? AND state='HELD'", args);
    }

    public int confirmBatch(Object... args) {
        return queries.update("UPDATE camerahub.reservation_batches SET state='CONFIRMED' WHERE id=? AND state='HELD'", args);
    }

    public <T> List<T> lockBatchProducts(Class<T> type, Object... args) {
        return queries.queryForList("SELECT product_id FROM camerahub.product_reservations WHERE batch_id=? AND state<>'RELEASED' ORDER BY product_id FOR UPDATE", type, args);
    }

    public int releaseBatchInventory(Object... args) {
        return queries.update("UPDATE camerahub.products SET listing_status='PUBLISHED',updated_at=now(),version=version+1 WHERE id=? AND listing_status='RESERVED'", args);
    }

    public int releaseBatchProduct(Object... args) {
        return queries.update("UPDATE camerahub.product_reservations SET state='RELEASED' WHERE product_id=? AND batch_id=?", args);
    }

    public int releaseBatch(Object... args) {
        return queries.update("UPDATE camerahub.reservation_batches SET state='RELEASED' WHERE id=?", args);
    }

    public <T> List<T> findSnapshots(Class<T> type, Object... args) {
        return queries.queryForList("SELECT snapshot::text FROM camerahub.product_reservations WHERE batch_id=? ORDER BY product_id", type, args);
    }

    public List<Map<String, Object>> lockReservation(Object... args) {
        return queries.queryForList("SELECT product_id FROM camerahub.product_reservations WHERE product_id=? AND batch_id=? FOR UPDATE", args);
    }

    public int releaseOrderInventory(Object... args) {
        return queries.update("UPDATE camerahub.products SET listing_status='PUBLISHED',updated_at=now(),version=version+1 WHERE id=? AND listing_status='RESERVED'", args);
    }

    public int releaseOrderProduct(Object... args) {
        return queries.update("UPDATE camerahub.product_reservations SET state='RELEASED' WHERE product_id=? AND batch_id=?", args);
    }
}

