package vn.camerahub.ai.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "advisor_conversations", schema = "camerahub")
public class AdvisorConversation {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "owner_key", nullable = false, length = 64)
    private String ownerKey;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "messages", nullable = false, columnDefinition = "jsonb")
    private String messages;
    @Column(name = "lease_token", nullable = true)
    private UUID leaseToken;
    @Column(name = "leased_until", nullable = true)
    private Instant leasedUntil;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID id() {
        return id;
    }

    public String messages() {
        return messages;
    }
}
