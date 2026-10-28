package vn.camerahub.identity.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "user_roles", schema = "camerahub")
@IdClass(UserRoleId.class)
public class UserRole {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Id
    @Column(name = "role_code", nullable = false, length = 20)
    private String roleCode;

}
