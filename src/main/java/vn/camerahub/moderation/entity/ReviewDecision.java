package vn.camerahub.moderation.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "review_decisions", schema = "camerahub")
public class ReviewDecision {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "listing_id", nullable = false)
    private UUID listingId;
    @Column(name = "revision", nullable = false)
    private Long revision;
    @Column(name = "status", nullable = false, length = 24)
    private String status;
    @Column(name = "note", nullable = false, columnDefinition = "text")
    private String note;
    @Column(name = "source", nullable = false, length = 12)
    private String source;
    @Column(name = "actor_id", nullable = true)
    private UUID actorId;
    @Column(name = "applied", nullable = false)
    private Boolean applied;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID id() {
        return id;
    }

    public UUID listingId() {
        return listingId;
    }

    public Long revision() {
        return revision;
    }

    public String status() {
        return status;
    }

    public String note() {
        return note;
    }

}
