package vn.camerahub.commerce.repository;

import java.util.*;

import vn.camerahub.shared.persistence.*;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
@lombok.RequiredArgsConstructor
public class ReviewRepositoryOperationsImpl implements ReviewRepositoryOperations {
    private final JpaQueries queries;

    public List<Map<String, Object>> findCheckoutBatches(Object... args) {
        return queries.queryForList("SELECT state FROM camerahub.checkout_batches WHERE id=?", args);
    }

    public List<Map<String, Object>> findOrders(Object... args) {
        return queries.queryForList("SELECT order_status AS status,product_id AS \"productId\",checkout_batch_id AS \"batchId\" FROM camerahub.orders WHERE id=?", args);
    }

    public Map<String, Object> findSellerReviews(Object... args) {
        return queries.queryForMap("SELECT count(*) AS \"reviewCount\",round(avg(r.rating),1) AS \"averageRating\" FROM camerahub.seller_reviews r JOIN camerahub.orders o ON o.id=r.order_id WHERE o.seller_id=?", args);
    }

    public List<Map<String, Object>> findSellerReviews2(Object... args) {
        return queries.queryForList("""
                SELECT r.id,r.rating,r.comment,r.created_at AS "createdAt",o.buyer_id,o.product_id AS "productId",o.product_snapshot->>'title' AS "productTitle"
                FROM camerahub.seller_reviews r JOIN camerahub.orders o ON o.id=r.order_id WHERE o.seller_id=? ORDER BY r.created_at DESC,r.id DESC LIMIT 12 OFFSET ?
                """, args);
    }

    public <T> T findSellerReviews3(Class<T> type, Object... args) {
        return queries.queryForObject("SELECT count(*) FROM camerahub.seller_reviews r JOIN camerahub.orders o ON o.id=r.order_id WHERE o.seller_id=?", type, args);
    }

    public List<Map<String, Object>> findOrders2(Object... args) {
        return queries.queryForList("SELECT order_status,seller_id FROM camerahub.orders WHERE id=? AND buyer_id=? FOR UPDATE", args);
    }

    public int insertSellerReviews(Object... args) {
        return queries.update("INSERT INTO camerahub.seller_reviews(id,order_id,rating,comment) VALUES (?,?,?,?)", args);
    }
}

