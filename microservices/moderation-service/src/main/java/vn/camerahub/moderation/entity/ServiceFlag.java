package vn.camerahub.moderation.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "service_flags", schema = "camerahub")
public class ServiceFlag {

    @Id
    @Column(name = "key", nullable = false, columnDefinition = "text")
    private String key;
    @Column(name = "value", nullable = false)
    private Boolean value;

    public String key() {
        return key;
    }

    public Boolean value() {
        return value;
    }

}
