package vn.camerahub.identity.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "users", schema = "camerahub")
public class Account {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "email", nullable = false, columnDefinition = "text")
    private String email;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    private String passwordHash;
    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;
    @Column(name = "phone", nullable = true, length = 20)
    private String phone;
    @Column(name = "account_status", nullable = false, length = 20)
    private String accountStatus;
    @Column(name = "email_verified_at", nullable = true)
    private Instant emailVerifiedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "profile_version", nullable = false)
    private Long profileVersion;

    @Column(name = "avatar_key")
    private UUID avatarKey;
    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "avatar_content", columnDefinition = "bytea")
    private byte[] avatarContent;
    @Column(name = "avatar_type")
    private String avatarType;

    public String getAvatarUrl() {
        return avatarKey == null ? null : "/api/auth/me/avatar?key=" + avatarKey;
    }

    @Transient
    private List<String> roles = List.of();

    public void setRoles(List<String> value) {
        roles = List.copyOf(value);
    }
}
