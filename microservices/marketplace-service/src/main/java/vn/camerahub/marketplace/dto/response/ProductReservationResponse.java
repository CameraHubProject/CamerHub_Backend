package vn.camerahub.marketplace.dto.response;

import java.util.*;
import java.time.*;

public record ProductReservationResponse(UUID productId, UUID batchId, UUID buyerId, UUID token, String snapshot,
                                         String state, Instant createdAt) {
}
