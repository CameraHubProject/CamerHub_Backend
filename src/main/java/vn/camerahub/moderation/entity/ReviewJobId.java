package vn.camerahub.moderation.entity;

import java.io.Serializable;
import java.util.UUID;
import java.time.*;

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ReviewJobId implements Serializable {
    private static final long serialVersionUID = 1L;
    private UUID listingId;
    private Long revision;
}
