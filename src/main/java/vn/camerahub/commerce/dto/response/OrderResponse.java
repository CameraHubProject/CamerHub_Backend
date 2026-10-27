package vn.camerahub.commerce.dto.response;

import java.util.*;
import java.time.*;

public record OrderResponse(UUID id, UUID productId, UUID buyerId, UUID sellerId, Long agreedPriceVnd,
                            Long shippingFeeVnd, Long transactionFeeVnd, Long totalVnd, String productSnapshot,
                            String shippingAddressSnapshot, String orderStatus, Instant paymentDeadline,
                            Instant createdAt, Instant updatedAt, Integer depositRateBps, Long depositAmountVnd,
                            Long balanceProductAmountVnd, Instant depositDeadline, Instant balanceDeadline,
                            UUID checkoutBatchId, Boolean releaseSynced) {
}
