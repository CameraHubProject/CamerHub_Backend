package vn.camerahub.marketplace.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "reservation_batches", schema = "camerahub")
public class ReservationBatch {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "product_ids", nullable = false, columnDefinition = "jsonb")
    private String productIds;
    @Column(name = "state", nullable = false, length = 20)
    private String state;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID id() {
        return id;
    }

    public UUID buyerId() {
        return buyerId;
    }

    public String productIds() {
        return productIds;
    }

}
