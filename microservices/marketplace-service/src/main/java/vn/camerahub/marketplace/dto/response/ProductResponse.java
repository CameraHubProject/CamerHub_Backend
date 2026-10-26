package vn.camerahub.marketplace.dto.response;

import java.util.*;
import java.time.*;

public record ProductResponse(UUID id, UUID sellerId, UUID modelId, String title, Integer shutterCount, Long priceVnd,
                              String description, String conditionNotes, String repairHistory, String accessories,
                              String listingStatus, String inspectionBadge, Integer version, UUID reviewedBy,
                              Instant reviewedAt, String rejectionReason, Instant createdAt, Instant updatedAt,
                              String presentation, String moderationSource, String moderationNote,
                              Long submittedRevision, String inspectionChecks) {
}
