package vn.camerahub.marketplace.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "product_status_history", schema = "camerahub")
public class ProductStatusHistory {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "old_status", nullable = true, columnDefinition = "text")
    private String oldStatus;
    @Column(name = "new_status", nullable = false, columnDefinition = "text")
    private String newStatus;
    @Column(name = "actor_id", nullable = true)
    private UUID actorId;
    @Column(name = "reason", nullable = true, columnDefinition = "text")
    private String reason;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public UUID id() {
        return id;
    }

    public UUID productId() {
        return productId;
    }

    public String reason() {
        return reason;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

}
