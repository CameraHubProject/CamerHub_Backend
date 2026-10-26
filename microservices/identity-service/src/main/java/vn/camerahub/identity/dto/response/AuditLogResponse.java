package vn.camerahub.identity.dto.response;

import java.util.*;
import java.time.*;

public record AuditLogResponse(UUID id, UUID actorId, String entityType, UUID entityId, String action, String oldData,
                               String newData, String correlationId, Instant occurredAt) {
}
