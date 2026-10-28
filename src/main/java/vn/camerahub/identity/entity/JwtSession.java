package vn.camerahub.identity.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "jwt_sessions", schema = "camerahub")
public class JwtSession {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "refresh_hash", nullable = false, length = 64, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String refreshHash;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(nullable = false)
    private boolean revoked;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public JwtSession(UUID id, UUID userId, String refreshHash, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.refreshHash = refreshHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

}
