package vn.camerahub.marketplace.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_media_data", schema = "camerahub")
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
public class ProductMediaData {
    @Id
    @Column(name = "media_id")
    private UUID mediaId;
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] content;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
