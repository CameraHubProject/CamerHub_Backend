package vn.camerahub.moderation.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "review_jobs", schema = "camerahub")
@IdClass(ReviewJobId.class)
public class ReviewJob {

    @Id
    @Column(name = "listing_id", nullable = false)
    private UUID listingId;
    @Id
    @Column(name = "revision", nullable = false)
    private Long revision;
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "snapshot", nullable = false, columnDefinition = "jsonb")
    private String snapshot;
    @Column(name = "state", nullable = false, length = 24)
    private String state;
    @Column(name = "note", nullable = true, columnDefinition = "text")
    private String note;
    @Column(name = "source", nullable = true, length = 12)
    private String source;
    @Column(name = "claim_token", nullable = true)
    private UUID claimToken;
    @Column(name = "leased_until", nullable = true)
    private Instant leasedUntil;
    @Column(name = "attempts", nullable = false)
    private Integer attempts;
    @Column(name = "decision_id", nullable = true)
    private UUID decisionId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID listingId() {
        return listingId;
    }

    public Long revision() {
        return revision;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String snapshot() {
        return snapshot;
    }

    public String note() {
        return note;
    }

    public Integer attempts() {
        return attempts;
    }

    public ReviewJob(UUID listingId, long revision, UUID ownerId, String snapshot) {
        this.listingId = listingId;
        this.revision = revision;
        this.ownerId = ownerId;
        this.snapshot = snapshot;
    }
}
