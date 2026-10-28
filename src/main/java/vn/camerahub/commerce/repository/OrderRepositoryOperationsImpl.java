package vn.camerahub.commerce.repository;

import java.util.*;

import vn.camerahub.commerce.entity.Order;
import vn.camerahub.shared.persistence.*;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
@lombok.RequiredArgsConstructor
public class OrderRepositoryOperationsImpl implements OrderRepositoryOperations {
    private final JpaQueries queries;

    private static final String ORDER_SELECT = """
            SELECT id,product_id AS "productId",buyer_id AS "buyerId",seller_id AS "sellerId",order_status AS status,
            agreed_price_vnd::text AS "priceVnd",shipping_fee_vnd::text AS "shippingFeeVnd",total_vnd::text AS "totalVnd",
            deposit_amount_vnd::text AS "depositVnd",balance_product_amount_vnd::text AS "balanceVnd",deposit_rate_bps AS "depositRateBps",
            product_snapshot->>'title' AS title,created_at AS "createdAt",payment_deadline AS "paymentDeadline",
            shipping_address_snapshot::text AS "addressJson" FROM camerahub.orders
            """;

    public List<Map<String, Object>> findDatabase(Object... args) {
        return queries.queryForList(ORDER_SELECT + " WHERE buyer_id=? OR seller_id=? ORDER BY created_at DESC", args);
    }

    public List<Map<String, Object>> findDatabase2(Object... args) {
        return queries.queryForList(ORDER_SELECT + " WHERE buyer_id=? AND checkout_batch_id=? ORDER BY id", args);
    }

    public List<Map<String, Object>> findDatabase3(Object... args) {
        return queries.queryForList(ORDER_SELECT + " WHERE id=? AND (buyer_id=? OR seller_id=?)", args);
    }

    public List<Map<String, Object>> findPaymentRecords(Object... args) {
        return queries.queryForList("SELECT payment_stage AS stage,amount_vnd::text AS \"amountVnd\",status,created_at AS \"createdAt\" FROM camerahub.payment_records WHERE order_id=? ORDER BY created_at", args);
    }

    public List<Map<String, Object>> findSellerReviews(Object... args) {
        return queries.queryForList("SELECT rating,comment,created_at AS \"createdAt\" FROM camerahub.seller_reviews WHERE order_id=?", args);
    }

    public <T> T findPaymentRecords2(Class<T> type, Object... args) {
        return queries.queryForObject("SELECT count(*) FROM camerahub.payment_records WHERE order_id=? AND payment_stage=? AND status='SIMULATED_PAID'", type, args);
    }

    public int insertPaymentRecords(Object... args) {
        return queries.update("INSERT INTO camerahub.payment_records(order_id,amount_vnd,status,idempotency_key,payment_stage,simulated_at) VALUES (?,?,'SIMULATED_PAID',?,?,now())", args);
    }

    public int updateOrderDeposits(Object... args) {
        return queries.update("UPDATE camerahub.order_deposits SET status=?,paid_at=COALESCE(paid_at,now()) WHERE order_id=?", args);
    }

    public int updateOrders(Object... args) {
        return queries.update("UPDATE camerahub.orders SET order_status=?,payment_deadline=now()+interval '24 hours',balance_deadline=CASE WHEN ? THEN now()+interval '24 hours' ELSE balance_deadline END,updated_at=now() WHERE id=?", args);
    }

    public List<Map<String, Object>> findBuybackRequests(Object... args) {
        return queries.queryForList("SELECT id,model,condition_notes AS \"conditionNotes\",expected_price_vnd::text AS \"expectedPriceVnd\",status,created_at AS \"createdAt\" FROM camerahub.buyback_requests WHERE user_id=? ORDER BY created_at DESC", args);
    }

    public int insertBuybackRequests(Object... args) {
        return queries.update("INSERT INTO camerahub.buyback_requests(id,user_id,model,condition_notes,expected_price_vnd) VALUES (?,?,?,?,?)", args);
    }
}

