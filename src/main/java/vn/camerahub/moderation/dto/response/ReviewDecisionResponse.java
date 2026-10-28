package vn.camerahub.moderation.dto.response;

import java.util.*;
import java.time.*;

public record ReviewDecisionResponse(UUID id, UUID listingId, Long revision, String status, String note, String source,
                                     UUID actorId, Boolean applied, Instant createdAt) {
}
