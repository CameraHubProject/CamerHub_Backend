package vn.camerahub.identity.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "user_addresses", schema = "camerahub")
public class UserAddress {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Account account;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "recipient_name", nullable = false, length = 160)
    private String recipientName;
    @Column(name = "phone", nullable = false, length = 20)
    private String phone;
    @Column(name = "province", nullable = false, columnDefinition = "text")
    private String province;
    @Column(name = "district", nullable = true, columnDefinition = "text")
    private String district;
    @Column(name = "ward", nullable = true, columnDefinition = "text")
    private String ward;
    @Column(name = "street_address", nullable = false, columnDefinition = "text")
    private String streetAddress;
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID id() {
        return id;
    }

    public String recipientName() {
        return recipientName;
    }

    public String phone() {
        return phone;
    }

    public String province() {
        return province;
    }

    public String streetAddress() {
        return streetAddress;
    }

    public Boolean isDefault() {
        return isDefault;
    }

}
