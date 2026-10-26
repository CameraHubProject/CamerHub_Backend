package vn.camerahub.ai.entity;

import java.io.Serializable;
import java.util.UUID;
import java.time.*;

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class AdvisorUsageId implements Serializable {
    private static final long serialVersionUID = 1L;
    private LocalDate day;
    private String clientKey;
}
