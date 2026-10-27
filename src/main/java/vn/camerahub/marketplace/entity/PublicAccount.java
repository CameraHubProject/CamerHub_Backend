package vn.camerahub.marketplace.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "public_accounts", schema = "camerahub")
public class PublicAccount {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;
    @Column(name = "account_status", nullable = false, length = 20)
    private String accountStatus;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "profile_version", nullable = false)
    private Long profileVersion;

    public UUID id() {
        return id;
    }

    public String fullName() {
        return fullName;
    }

}
