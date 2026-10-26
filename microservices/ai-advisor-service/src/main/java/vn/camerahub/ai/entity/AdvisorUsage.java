package vn.camerahub.ai.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "advisor_usage", schema = "camerahub")
@IdClass(AdvisorUsageId.class)
public class AdvisorUsage {

    @Id
    @Column(name = "day", nullable = false)
    private LocalDate day;
    @Id
    @Column(name = "client_key", nullable = false, length = 64)
    private String clientKey;
    @Column(name = "requests", nullable = false)
    private Integer requests;

}
