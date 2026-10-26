package vn.camerahub.marketplace.dto.response;

import java.util.*;
import java.time.*;

public record ReservationBatchResponse(UUID id, UUID buyerId, String productIds, String state, Instant createdAt) {
}
