package vn.camerahub.marketplace.dto.response;

import java.util.*;
import java.time.*;

public record ProductMediaResponse(UUID id, UUID productId, String storageKey, String mediaType, Integer sortOrder,
                                   Instant createdAt, String imageSha256, Long imageDhash) {
}
