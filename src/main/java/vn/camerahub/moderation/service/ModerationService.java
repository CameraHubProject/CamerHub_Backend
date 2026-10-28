package vn.camerahub.moderation.service;

import vn.camerahub.moderation.exception.*;

import vn.camerahub.moderation.service.DuplicateIndex;
import vn.camerahub.moderation.service.ListingAiReviewer;
import vn.camerahub.moderation.repository.ModerationRepository;

import com.fasterxml.jackson.databind.*;

import java.time.Instant;
import java.util.*;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;

import vn.camerahub.shared.*;

@Service
public class ModerationService {
    private final ModerationRepository repository;
    private final ObjectMapper json;
    private final IntegrationEvents events;
    private final InternalClient client;
    private final DuplicateIndex duplicates;
    private final ListingAiReviewer reviewer;
    private final TransactionTemplate tx;

    public ModerationService(ModerationRepository repository, ObjectMapper json, IntegrationEvents events, InternalClient client,
                             DuplicateIndex duplicates, ListingAiReviewer reviewer, PlatformTransactionManager manager) {
        this.repository = repository;
        this.json = json;
        this.events = events;
        this.client = client;
        this.duplicates = duplicates;
        this.reviewer = reviewer;
        this.tx = new TransactionTemplate(manager);
    }

    private void lockIndex() {
        repository.lockDuplicateIndex();
    }

    public void receive(IntegrationEvents.Event event) {
        if (!"marketplace-service".equals(event.producer())) throw new IllegalArgumentException("Invalid producer");
        events.receive(event, () -> {
            lockIndex();
            switch (event.eventType()) {
                case "listing.submitted.v1", "listing.state.v1" -> {
                    if (!event.aggregateId().toString().equals(event.payload().path("id").asText()))
                        throw new IllegalArgumentException("Invalid listing");
                    boolean accepted = index(event.payload(), event.occurredAt());
                    if (accepted && "listing.submitted.v1".equals(event.eventType())) queue(event.payload());
                }
                case "listing.applied.v1" -> {
                    UUID decision = UUID.fromString(event.payload().path("decisionId").asText());
                    repository.acknowledgeDecision(event.payload().path("applied").asBoolean(), decision, event.aggregateId(), event.aggregateVersion());
                }
                default -> throw new IllegalArgumentException("Unsupported event");
            }
        });
    }

    private boolean index(JsonNode snapshot, Instant occurred) {
        UUID id = UUID.fromString(snapshot.path("id").asText());
        long revision = snapshot.path("revision").asLong();
        return repository.upsertIndex(id, revision, events.encode(snapshot), snapshot.path("listingStatus").asText(), occurred.atOffset(java.time.ZoneOffset.UTC)) == 1;
    }

    private void queue(JsonNode snapshot) {
        UUID id = UUID.fromString(snapshot.path("id").asText());
        long revision = snapshot.path("revision").asLong();
        repository.supersedeOldRevisions(id, revision);
        repository.enqueueRevision(id, revision, UUID.fromString(snapshot.path("sellerId").asText()), events.encode(snapshot));
    }

    /**
     * Import completes before this flag is enabled; workers never approve against an empty historical index.
     */
    public Object bootstrap() {
        int offset = 0;
        while (true) {
            var page = client.get("marketplace", "/internal/index-snapshots?offset=" + offset);
            tx.executeWithoutResult(s -> {
                lockIndex();
                for (var snapshot : page) {
                    index(snapshot, Instant.EPOCH);
                    if ("AI_REVIEW".equals(snapshot.path("listingStatus").asText())) queue(snapshot);
                    if ("PENDING_REVIEW".equals(snapshot.path("listingStatus").asText())) {
                        queue(snapshot);
                        repository.restoreHumanQueue(UUID.fromString(snapshot.path("id").asText()), snapshot.path("revision").asLong());
                    }
                }
            });
            offset += page.size();
            if (page.size() < 100) break;
        }
        repository.markIndexReady();
        return Map.of("indexed", offset);
    }

    @Scheduled(fixedDelayString = "${camerahub.moderation.poll-ms:1500}")
    public void work() {
        if (!Boolean.TRUE.equals(repository.isIndexReady(Boolean.class))) return;
        UUID token = UUID.randomUUID();
        var job = tx.execute(s -> {
            var rows = repository.lockNextJob();
            if (rows.isEmpty()) return null;
            var row = rows.getFirst();
            return repository.claimJob(token, row.get("listing_id"), row.get("revision"));
        });
        if (job == null) return;
        try {
            var snapshot = json.readTree(job.snapshot());
            String reason = duplicates.reason(snapshot);
            var verdict = reason == null ? reviewer.review(json.convertValue(snapshot, Map.class), DuplicateIndex.imageIds(snapshot), duplicates.candidates(snapshot)) : new ListingAiReviewer.Decision("REJECTED", reason);
            if ("PUBLISHED".equals(verdict.status()) && !"ACTIVE".equals(client.get("identity", "/internal/accounts/" + job.ownerId()).path("status").asText()))
                verdict = new ListingAiReviewer.Decision("PENDING_REVIEW", "Tài khoản người bán cần được kiểm tra.");
            var decision = verdict;
            tx.executeWithoutResult(s -> {
                lockIndex();
                var rows = repository.lockClaim(job.listingId(), job.revision());
                if (rows.isEmpty() || !"PROCESSING".equals(rows.getFirst().get("state")) || !token.equals(rows.getFirst().get("claim_token")))
                    return;
                String repeated = duplicates.reason(snapshot);
                decide(snapshot, repeated == null ? decision.status() : "REJECTED", repeated == null ? decision.note() : repeated, "AI", null);
            });
        } catch (Exception e) {
            tx.executeWithoutResult(s -> {
                lockIndex();
                var rows = repository.findOwnedClaimSnapshot(String.class, job.listingId(), job.revision(), token);
                if (!rows.isEmpty()) try {
                    decide(json.readTree(rows.getFirst()), "PENDING_REVIEW", "AI chưa hoàn tất kiểm tra; đã chuyển Admin duyệt.", "AI", null);
                } catch (java.io.IOException error) {
                    throw new IllegalStateException(error);
                }
            });
        }
    }

    private void decide(JsonNode snapshot, String status, String note, String source, UUID actor) {
        UUID id = UUID.fromString(snapshot.path("id").asText()), decision = UUID.randomUUID();
        long revision = snapshot.path("revision").asLong();
        repository.insertDecision(decision, id, revision, status, note, source, actor);
        repository.finishJob(status, note, source, decision, id, revision);
        var indexed = snapshot.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) indexed).put("listingStatus", status);
        index(indexed, Instant.now());
        events.publish("moderation.decided.v1", id, revision, Map.of("decisionId", decision, "status", status, "note", note, "source", source));
    }

    public Object queue() {
        var items = new ArrayList<Map<String, Object>>();
        for (var row : repository.findHumanQueue()) {
            try {
                var item = json.convertValue(json.readTree(row.get("snapshot").toString()), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                });
                item.put("images", DuplicateIndex.imageIds(json.valueToTree(item)).stream().map(id -> "/api/public/product-media/" + id).toList());
                item.put("listingStatus", "PENDING_REVIEW");
                item.put("moderationRevision", item.get("revision"));
                item.put("moderationNote", row.get("note"));
                items.add(item);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return items;
    }

    public Object review(UUID actor, UUID id, long revision, boolean approve, String note) {
        return tx.execute(s -> {
            lockIndex();
            var rows = repository.lockHumanReview(id, revision);
            if (rows.isEmpty())
                throw new ModerationException(ModerationErrorCode.REVIEW_REVISION_CHANGED);
            try {
                var snapshot = json.readTree(rows.getFirst().get("snapshot").toString());
                if (approve) {
                    if (!"ACTIVE".equals(client.get("identity", "/internal/accounts/" + rows.getFirst().get("owner_id")).path("status").asText()))
                        throw new ModerationException(ModerationErrorCode.SELLER_INACTIVE);
                    String duplicate = duplicates.reason(snapshot);
                    if (duplicate != null) throw new ModerationException(ModerationErrorCode.DUPLICATE_LISTING);
                }
                decide(snapshot, approve ? "PUBLISHED" : "NEEDS_REVISION", note.strip(), "ADMIN", actor);
                return Map.of("accepted", true);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }
}

