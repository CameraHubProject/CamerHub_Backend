package vn.camerahub.marketplace.dto.response;

import java.util.*;
import java.time.*;

public record ProductStatusHistoryResponse(UUID id, UUID productId, String oldStatus, String newStatus, UUID actorId,
                                           String reason, Instant occurredAt) {
}
