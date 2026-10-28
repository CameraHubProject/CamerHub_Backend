package vn.camerahub.commerce.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "seller_reviews", schema = "camerahub")
public class SellerReview {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Order order;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "order_id", nullable = false)
    private UUID orderId;
    @Column(name = "rating", nullable = false)
    private Short rating;
    @Column(name = "comment", nullable = false, columnDefinition = "text")
    private String comment;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID id() {
        return id;
    }

    public Short rating() {
        return rating;
    }

    public String comment() {
        return comment;
    }

}
