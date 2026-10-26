package vn.camerahub.identity.dto.response;

import java.util.UUID;
import java.time.Instant;

public record PublicAccountResponse(UUID id, String fullName, String status, Instant joinedAt, long version) {
}
