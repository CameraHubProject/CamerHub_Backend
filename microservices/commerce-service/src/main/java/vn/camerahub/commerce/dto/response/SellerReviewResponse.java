package vn.camerahub.commerce.dto.response;

import java.util.*;
import java.time.*;

public record SellerReviewResponse(UUID id, UUID orderId, Short rating, String comment, Instant createdAt) {
}
