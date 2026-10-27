package vn.camerahub.commerce.dto.response;

import java.util.*;
import java.time.*;

public record CheckoutBatchResponse(UUID id, UUID buyerId, String requestHash, String state, String request,
                                    String reservations, String lastError, Instant createdAt, Instant updatedAt,
                                    Instant leasedUntil, UUID leaseToken, Boolean reservationSynced) {
}
