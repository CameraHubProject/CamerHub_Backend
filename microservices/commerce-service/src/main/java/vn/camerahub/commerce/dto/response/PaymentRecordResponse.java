package vn.camerahub.commerce.dto.response;

import java.util.*;
import java.time.*;

public record PaymentRecordResponse(UUID id, UUID orderId, Long amountVnd, String paymentMode, String status,
                                    String idempotencyKey, Instant simulatedAt, Instant createdAt,
                                    String paymentStage) {
}
