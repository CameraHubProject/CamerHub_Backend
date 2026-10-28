package vn.camerahub.moderation.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "listing_index", schema = "camerahub")
public class ListingIndex {

    @Id
    @Column(name = "listing_id", nullable = false)
    private UUID listingId;
    @Column(name = "revision", nullable = false)
    private Long revision;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "snapshot", nullable = false, columnDefinition = "jsonb")
    private String snapshot;
    @Column(name = "listing_status", nullable = false, length = 24)
    private String listingStatus;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID listingId() {
        return listingId;
    }

    public Long revision() {
        return revision;
    }

    public String snapshot() {
        return snapshot;
    }

    public String listingStatus() {
        return listingStatus;
    }

}
