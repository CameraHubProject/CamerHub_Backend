package vn.camerahub.moderation.dto.response;

import java.util.*;
import java.time.*;

public record ListingIndexResponse(UUID listingId, Long revision, String snapshot, String listingStatus,
                                   Instant updatedAt) {
}
