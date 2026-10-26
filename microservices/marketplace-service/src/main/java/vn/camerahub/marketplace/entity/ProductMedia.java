package vn.camerahub.marketplace.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "product_media", schema = "camerahub")
public class ProductMedia {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Product product;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "storage_key", nullable = false, columnDefinition = "text")
    private String storageKey;
    @Column(name = "media_type", nullable = false, length = 12)
    private String mediaType;
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "image_sha256", nullable = true, length = 64)
    private String imageSha256;
    @Column(name = "image_dhash", nullable = true)
    private Long imageDhash;
    @Column(name = "image_signature", nullable = true, columnDefinition = "text")
    private String imageSignature;

    public UUID id() {
        return id;
    }

    public UUID productId() {
        return productId;
    }

}
