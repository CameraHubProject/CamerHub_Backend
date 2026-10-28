package vn.camerahub.commerce.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "checkout_batches", schema = "camerahub")
public class CheckoutBatch {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;
    @Column(name = "request_hash", nullable = false, columnDefinition = "text")
    private String requestHash;
    @Column(name = "state", nullable = false, length = 20)
    private String state;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "request", nullable = false, columnDefinition = "jsonb")
    private String request;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "reservations", nullable = false, columnDefinition = "jsonb")
    private String reservations;
    @Column(name = "last_error", nullable = true, columnDefinition = "text")
    private String lastError;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "leased_until", nullable = true)
    private Instant leasedUntil;
    @Column(name = "lease_token", nullable = true)
    private UUID leaseToken;
    @Column(name = "reservation_synced", nullable = false)
    private Boolean reservationSynced;

    public UUID id() {
        return id;
    }

    public UUID buyerId() {
        return buyerId;
    }

}
