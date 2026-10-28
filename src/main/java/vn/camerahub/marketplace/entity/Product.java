package vn.camerahub.marketplace.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "products", schema = "camerahub")
public class Product {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private CameraModel model;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;
    @Column(name = "model_id", nullable = false)
    private UUID modelId;
    @Column(name = "title", nullable = false, length = 240)
    private String title;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "serial_number_private", nullable = true, columnDefinition = "text")
    private String serialNumberPrivate;
    @Column(name = "serial_last4", nullable = true, length = 4)
    private String serialLast4;
    @Column(name = "shutter_count", nullable = true)
    private Integer shutterCount;
    @Column(name = "price_vnd", nullable = false)
    private Long priceVnd;
    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;
    @Column(name = "condition_notes", nullable = false, columnDefinition = "text")
    private String conditionNotes;
    @Column(name = "repair_history", nullable = true, columnDefinition = "text")
    private String repairHistory;
    @Column(name = "accessories", nullable = true, columnDefinition = "text")
    private String accessories;
    @Column(name = "listing_status", nullable = false, length = 24)
    private String listingStatus;
    @Column(name = "inspection_badge", nullable = false, length = 20)
    private String inspectionBadge;
    @Column(name = "version", nullable = false)
    private Integer version;
    @Column(name = "reviewed_by", nullable = true)
    private UUID reviewedBy;
    @Column(name = "reviewed_at", nullable = true)
    private Instant reviewedAt;
    @Column(name = "rejection_reason", nullable = true, columnDefinition = "text")
    private String rejectionReason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "presentation", nullable = false, columnDefinition = "jsonb")
    private String presentation;
    @Column(name = "moderation_source", nullable = true, length = 12)
    private String moderationSource;
    @Column(name = "moderation_note", nullable = true, columnDefinition = "text")
    private String moderationNote;
    @Column(name = "submitted_revision", nullable = false)
    private Long submittedRevision;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "inspection_checks", nullable = false, columnDefinition = "jsonb")
    private String inspectionChecks;

    public UUID id() {
        return id;
    }

    public UUID sellerId() {
        return sellerId;
    }

    public UUID modelId() {
        return modelId;
    }

    public String title() {
        return title;
    }

    public Integer shutterCount() {
        return shutterCount;
    }

    public Long priceVnd() {
        return priceVnd;
    }

    public String description() {
        return description;
    }

    public String conditionNotes() {
        return conditionNotes;
    }

    public String accessories() {
        return accessories;
    }

    public String listingStatus() {
        return listingStatus;
    }

    public Integer version() {
        return version;
    }
}
