package vn.camerahub.identity.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "roles", schema = "camerahub")
public class Role {

    @Id
    @Column(name = "code", nullable = false, length = 20)
    private String code;

}
