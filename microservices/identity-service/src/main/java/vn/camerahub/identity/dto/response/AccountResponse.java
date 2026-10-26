package vn.camerahub.identity.dto.response;

import java.util.*;
import java.time.*;

public record AccountResponse(UUID id, String email, String fullName, String phone, String accountStatus,
                              Instant emailVerifiedAt, Instant createdAt, Instant updatedAt, Long profileVersion) {
}
