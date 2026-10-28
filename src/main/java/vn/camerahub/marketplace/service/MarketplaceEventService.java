package vn.camerahub.marketplace.service;

import vn.camerahub.marketplace.service.ListingSubmission;
import vn.camerahub.marketplace.repository.MarketplaceEventRepository;

import java.time.Instant;
import java.util.*;

import org.springframework.web.bind.annotation.*;
import vn.camerahub.shared.*;

@org.springframework.stereotype.Service
@lombok.RequiredArgsConstructor
public class MarketplaceEventService {
    private final MarketplaceEventRepository repository;
    private final IntegrationEvents events;
    private final ListingSubmission submissions;

    public Object receive(IntegrationEvents.Event event) {
        events.receive(event, () -> {
            var p = event.payload();
            switch (event.eventType()) {
                case "identity.profile.v1" -> {
                    if (!"identity-service".equals(event.producer()))
                        throw new IllegalArgumentException("Invalid producer");
                    repository.applyPublicAccountProjection(event.aggregateId(), p.path("fullName").asText(), p.path("status").asText(), Instant.parse(p.path("joinedAt").asText()).atOffset(java.time.ZoneOffset.UTC), event.aggregateVersion());
                }
                case "moderation.decided.v1" -> {
                    if (!"moderation-service".equals(event.producer()))
                        throw new IllegalArgumentException("Invalid producer");
                    String status = p.path("status").asText();
                    if (!Set.of("PUBLISHED", "REJECTED", "PENDING_REVIEW", "NEEDS_REVISION").contains(status))
                        throw new IllegalArgumentException("Invalid decision");
                    UUID id = event.aggregateId();
                    int applied = repository.applyModerationDecision(status, p.path("source").asText(), p.path("note").asText(), status, p.path("note").asText(), id, event.aggregateVersion());
                    if (applied == 1) repository.insertProductStatusHistory(id, status, p.path("note").asText());
                    events.publish("listing.applied.v1", id, event.aggregateVersion(), Map.of("decisionId", p.path("decisionId").asText(), "applied", applied == 1, "status", status));
                }
                default -> throw new IllegalArgumentException("Unsupported event");
            }
        });
        return Map.of("accepted", true);
    }

    public Object snapshots(int offset) {
        if (offset < 0) throw new IllegalArgumentException("Invalid offset");
        var ids = repository.findIndexPage(UUID.class, offset);
        return ids.stream().map(submissions::snapshot).toList();
    }
}

