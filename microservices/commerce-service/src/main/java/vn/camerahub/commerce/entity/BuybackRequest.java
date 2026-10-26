package vn.camerahub.commerce.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "buyback_requests", schema = "camerahub")
public class BuybackRequest {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "model", nullable = false, length = 160)
    private String model;
    @Column(name = "condition_notes", nullable = false, columnDefinition = "text")
    private String conditionNotes;
    @Column(name = "expected_price_vnd", nullable = true)
    private Long expectedPriceVnd;
    @Column(name = "status", nullable = false, length = 24)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID id() {
        return id;
    }

    public String model() {
        return model;
    }

    public String conditionNotes() {
        return conditionNotes;
    }

    public Long expectedPriceVnd() {
        return expectedPriceVnd;
    }

    public String status() {
        return status;
    }

}
