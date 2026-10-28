package vn.camerahub.marketplace.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "product_reservations", schema = "camerahub")
public class ProductReservation {

    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "batch_id", nullable = false)
    private UUID batchId;
    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;
    @Column(name = "token", nullable = false)
    private UUID token;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "snapshot", nullable = false, columnDefinition = "jsonb")
    private String snapshot;
    @Column(name = "state", nullable = false, length = 20)
    private String state;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID productId() {
        return productId;
    }

    public UUID buyerId() {
        return buyerId;
    }

    public UUID token() {
        return token;
    }

    public String snapshot() {
        return snapshot;
    }

}
