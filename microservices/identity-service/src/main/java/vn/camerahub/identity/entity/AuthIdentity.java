package vn.camerahub.identity.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "auth_identities", schema = "camerahub")
@IdClass(AuthIdentityId.class)
public class AuthIdentity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Account account;

    @Id
    @Column(name = "provider", nullable = false, length = 20)
    private String provider;
    @Id
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public String subject() {
        return subject;
    }

}
