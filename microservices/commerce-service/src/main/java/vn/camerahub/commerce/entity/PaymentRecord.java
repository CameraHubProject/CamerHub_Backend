package vn.camerahub.commerce.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "payment_records", schema = "camerahub")
public class PaymentRecord {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Order order;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    @Column(name = "amount_vnd", nullable = false)
    private Long amountVnd;
    @Column(name = "payment_mode", nullable = false, length = 20)
    private String paymentMode;
    @Column(name = "status", nullable = false, length = 28)
    private String status;
    @Column(name = "idempotency_key", nullable = false, columnDefinition = "text")
    private String idempotencyKey;
    @Column(name = "simulated_at", nullable = true)
    private Instant simulatedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "payment_stage", nullable = false, length = 24)
    private String paymentStage;

    public UUID id() {
        return id;
    }

    public String status() {
        return status;
    }

}
