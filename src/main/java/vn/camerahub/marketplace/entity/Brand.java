package vn.camerahub.marketplace.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "brands", schema = "camerahub")
public class Brand {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

}
