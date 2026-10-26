package vn.camerahub.micro;

import com.fasterxml.jackson.databind.*;

import java.time.Instant;
import java.util.*;

import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

/**
 * Durable HTTP transport. Every destination has an independent delivery record and retry lease.
 */
@Component
@EnableScheduling
public class IntegrationEvents {
    public record Event(UUID eventId, String eventType, String producer, UUID aggregateId, long aggregateVersion,
                        Instant occurredAt, JsonNode payload) {
    }

    private static final Logger log = LoggerFactory.getLogger(IntegrationEvents.class);
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final InternalClient client;
    private final TransactionTemplate transactions;
    private final String service;
    private final boolean enabled;

    public IntegrationEvents(JdbcTemplate jdbc, ObjectMapper json, InternalClient client, PlatformTransactionManager manager,
                             @Value("${spring.application.name}") String service, @Value("${camerahub.events.enabled:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.json = json;
        this.client = client;
        this.transactions = new TransactionTemplate(manager);
        this.service = service;
        this.enabled = enabled;
    }

    public void publish(String type, UUID aggregate, long version, Object payload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Outbox requires the business transaction");
        List<String> targets = switch (type) {
            case "identity.profile.v1" -> List.of("marketplace");
            case "listing.submitted.v1", "listing.state.v1" -> List.of("moderation");
            case "moderation.decided.v1" -> List.of("marketplace");
            case "listing.applied.v1" -> List.of("moderation");
            default -> throw new IllegalArgumentException("Unknown event type");
        };
        var event = new Event(UUID.randomUUID(), type, service, aggregate, version, Instant.now(), json.valueToTree(payload));
        for (String target : targets)
            jdbc.update("INSERT INTO camerahub.integration_outbox(event_id,target,payload) VALUES (?,?,?::jsonb)", event.eventId(), target, encode(event));
    }

    public void receive(Event event, Runnable action) {
        if (event == null || event.eventId() == null || event.aggregateId() == null || event.eventType() == null || event.payload() == null || event.aggregateVersion() < 0)
            throw new IllegalArgumentException("Invalid event envelope");
        transactions.executeWithoutResult(tx -> {
            int created = jdbc.update("INSERT INTO camerahub.integration_inbox(event_id,event_type) VALUES (?,?) ON CONFLICT DO NOTHING", event.eventId(), event.eventType());
            if (created == 1) action.run();
        });
    }

    @Scheduled(fixedDelayString = "${camerahub.events.poll-ms:1000}")
    public void dispatch() {
        if (!enabled) return;
        try {
            var row = transactions.execute(tx -> {
                var candidates = jdbc.queryForList("SELECT event_id,target FROM camerahub.integration_outbox WHERE delivered_at IS NULL AND attempts<12 AND next_attempt_at<=now() AND (leased_until IS NULL OR leased_until<now()) ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED");
                if (candidates.isEmpty()) return null;
                var candidate = candidates.getFirst();
                return jdbc.queryForMap("UPDATE camerahub.integration_outbox SET leased_until=now()+interval '45 seconds',lease_token=?,attempts=attempts+1 WHERE event_id=? AND target=? RETURNING *", UUID.randomUUID(), candidate.get("event_id"), candidate.get("target"));
            });
            if (row == null) return;
            try {
                var event = json.readValue(row.get("payload").toString(), Event.class);
                client.post((String) row.get("target"), "/internal/events", event);
                jdbc.update("UPDATE camerahub.integration_outbox SET delivered_at=now(),leased_until=NULL WHERE event_id=? AND target=? AND lease_token=?", row.get("event_id"), row.get("target"), row.get("lease_token"));
            } catch (Exception e) {
                jdbc.update("UPDATE camerahub.integration_outbox SET leased_until=NULL,next_attempt_at=now()+least(attempts*attempts,300)*interval '1 second' WHERE event_id=? AND target=? AND lease_token=?", row.get("event_id"), row.get("target"), row.get("lease_token"));
                log.warn("Event delivery to {} failed; retained for retry (attempt {})", row.get("target"), row.get("attempts"));
            }
        } catch (Exception e) {
            log.warn("Outbox dispatch temporarily unavailable: {}", e.getClass().getSimpleName());
        }
    }

    public String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON data", e);
        }
    }
}

