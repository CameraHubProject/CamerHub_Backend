package vn.camerahub.commerce.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "order_deposits", schema = "camerahub")
public class OrderDeposit {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Order order;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    @Column(name = "deposit_rate_bps", nullable = false)
    private Integer depositRateBps;
    @Column(name = "amount_vnd", nullable = false)
    private Long amountVnd;
    @Column(name = "status", nullable = false, length = 22)
    private String status;
    @Column(name = "paid_at", nullable = true)
    private Instant paidAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID id() {
        return id;
    }

    public String status() {
        return status;
    }

}
