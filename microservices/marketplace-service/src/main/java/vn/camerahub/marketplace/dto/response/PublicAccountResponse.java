package vn.camerahub.marketplace.dto.response;

import java.util.*;
import java.time.*;

public record PublicAccountResponse(UUID id, String fullName, String accountStatus, Instant createdAt,
                                    Long profileVersion) {
}
