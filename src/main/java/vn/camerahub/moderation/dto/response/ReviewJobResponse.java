package vn.camerahub.moderation.dto.response;

import java.util.*;
import java.time.*;

public record ReviewJobResponse(UUID listingId, Long revision, UUID ownerId, String snapshot, String state, String note,
                                String source, UUID claimToken, Instant leasedUntil, Integer attempts, UUID decisionId,
                                Instant createdAt, Instant updatedAt) {
}
