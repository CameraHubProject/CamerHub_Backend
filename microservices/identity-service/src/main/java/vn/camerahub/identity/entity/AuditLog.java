package vn.camerahub.identity.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "audit_logs", schema = "camerahub")
public class AuditLog {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "actor_id", nullable = true)
    private UUID actorId;
    @Column(name = "entity_type", nullable = false, length = 80)
    private String entityType;
    @Column(name = "entity_id", nullable = false)
    private UUID entityId;
    @Column(name = "action", nullable = false, length = 80)
    private String action;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "old_data", nullable = true, columnDefinition = "jsonb")
    private String oldData;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "new_data", nullable = true, columnDefinition = "jsonb")
    private String newData;
    @Column(name = "correlation_id", nullable = true, columnDefinition = "text")
    private String correlationId;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public UUID id() {
        return id;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

}
