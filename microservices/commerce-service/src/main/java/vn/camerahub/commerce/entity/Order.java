package vn.camerahub.commerce.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "orders", schema = "camerahub")
public class Order {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;
    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;
    @Column(name = "agreed_price_vnd", nullable = false)
    private Long agreedPriceVnd;
    @Column(name = "shipping_fee_vnd", nullable = false)
    private Long shippingFeeVnd;
    @Column(name = "transaction_fee_vnd", nullable = false)
    private Long transactionFeeVnd;
    @Column(name = "total_vnd", nullable = true, insertable = false, updatable = false)
    private Long totalVnd;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "product_snapshot", nullable = false, columnDefinition = "jsonb")
    private String productSnapshot;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "shipping_address_snapshot", nullable = false, columnDefinition = "jsonb")
    private String shippingAddressSnapshot;
    @Column(name = "order_status", nullable = false, length = 26)
    private String orderStatus;
    @Column(name = "payment_deadline", nullable = false)
    private Instant paymentDeadline;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "deposit_rate_bps", nullable = false)
    private Integer depositRateBps;
    @Column(name = "deposit_amount_vnd", nullable = true, insertable = false, updatable = false)
    private Long depositAmountVnd;
    @Column(name = "balance_product_amount_vnd", nullable = true, insertable = false, updatable = false)
    private Long balanceProductAmountVnd;
    @Column(name = "deposit_deadline", nullable = true)
    private Instant depositDeadline;
    @Column(name = "balance_deadline", nullable = true)
    private Instant balanceDeadline;
    @Column(name = "checkout_batch_id", nullable = true)
    private UUID checkoutBatchId;
    @Column(name = "release_synced", nullable = false)
    private Boolean releaseSynced;

    public UUID id() {
        return id;
    }

    public UUID productId() {
        return productId;
    }

    public UUID buyerId() {
        return buyerId;
    }

    public UUID sellerId() {
        return sellerId;
    }

    public Long shippingFeeVnd() {
        return shippingFeeVnd;
    }

    public Long depositAmountVnd() {
        return depositAmountVnd;
    }

    @Transient
    private boolean expired;

    public boolean expired() {
        return expired || (paymentDeadline != null && paymentDeadline.isBefore(Instant.now()));
    }

    public String status() {
        return orderStatus;
    }

    public Long balanceAmountVnd() {
        return balanceProductAmountVnd;
    }
}
