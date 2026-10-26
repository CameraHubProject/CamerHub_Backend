package vn.camerahub.ai.entity;

import jakarta.persistence.*;

import java.time.*;
import java.util.*;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@Entity
@Table(name = "advisor_rate_windows", schema = "camerahub")
@IdClass(AdvisorRateWindowId.class)
public class AdvisorRateWindow {

    @Id
    @Column(name = "client_key", nullable = false, length = 64)
    private String clientKey;
    @Id
    @Column(name = "minute", nullable = false)
    private Long minute;
    @Column(name = "requests", nullable = false)
    private Integer requests;

    public Long minute() {
        return minute;
    }

}
