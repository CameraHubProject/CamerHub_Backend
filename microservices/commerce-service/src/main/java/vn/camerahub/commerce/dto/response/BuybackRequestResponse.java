package vn.camerahub.commerce.dto.response;

import java.util.*;
import java.time.*;

public record BuybackRequestResponse(UUID id, UUID userId, String model, String conditionNotes, Long expectedPriceVnd,
                                     String status, Instant createdAt) {
}
