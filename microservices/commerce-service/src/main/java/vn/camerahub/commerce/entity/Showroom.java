package vn.camerahub.commerce.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "showrooms", schema = "camerahub")
public class Showroom {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "name", nullable = false, length = 160)
    private String name;
    @Column(name = "address", nullable = false, columnDefinition = "text")
    private String address;
    @Column(name = "active", nullable = false)
    private Boolean active;

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String address() {
        return address;
    }

}
