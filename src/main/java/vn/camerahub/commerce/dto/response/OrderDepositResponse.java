package vn.camerahub.commerce.dto.response;

import java.util.*;
import java.time.*;

public record OrderDepositResponse(UUID id, UUID orderId, Integer depositRateBps, Long amountVnd, String status,
                                   Instant paidAt, Instant createdAt) {
}
