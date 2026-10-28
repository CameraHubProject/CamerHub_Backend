package vn.camerahub.ai.dto.response;

import java.util.*;
import java.time.*;

public record AdvisorConversationResponse(UUID id, String ownerKey, String messages, UUID leaseToken,
                                          Instant leasedUntil, Instant updatedAt) {
}
