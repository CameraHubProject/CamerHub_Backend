package vn.camerahub.marketplace.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "camera_models", schema = "camerahub")
public class CameraModel {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", insertable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Brand brand;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @Column(name = "brand_id", nullable = false)
    private UUID brandId;
    @Column(name = "model_name", nullable = false, length = 160)
    private String modelName;
    @Column(name = "device_type", nullable = false, length = 20)
    private String deviceType;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "specs", nullable = false, columnDefinition = "jsonb")
    private String specs;

    public UUID id() {
        return id;
    }

    public UUID brandId() {
        return brandId;
    }

    public String deviceType() {
        return deviceType;
    }

}
