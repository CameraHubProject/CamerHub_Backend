package vn.camerahub.marketplace.service;

import vn.camerahub.marketplace.exception.*;

import vn.camerahub.marketplace.util.ImageFingerprint;
import vn.camerahub.marketplace.repository.ListingRepository;
import vn.camerahub.marketplace.service.MarketplaceService;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.camerahub.micro.*;

@Service
public class ListingSubmission {
    private final ListingRepository repository;
    private final InternalClient client;
    private final IntegrationEvents events;
    private final String key;
    private final Path mediaRoot;

    public ListingSubmission(ListingRepository repository, InternalClient client, IntegrationEvents events, @Value("${camerahub.internal.key}") String key,
                             @Value("${camerahub.media-root:storage/product-media}") String root) {
        this.repository = repository;
        this.client = client;
        this.events = events;
        this.key = key;
        this.mediaRoot = Path.of(root);
    }

    public void ensureAccount(UUID id) {
        var account = client.get("identity", "/internal/accounts/" + id);
        if (!"ACTIVE".equals(account.path("status").asText()))
            throw new ListingException(MarketplaceErrorCode.ACCOUNT_INACTIVE);
        repository.upsertPublicAccount(id, account.path("fullName").asText(), account.path("status").asText(), Instant.parse(account.path("joinedAt").asText()).atOffset(java.time.ZoneOffset.UTC), account.path("version").asLong());
    }

    @Transactional
    public Map<String, Object> submit(UUID owner, UUID id) {
        var rows = repository.lockSubmission(id, owner);
        if (rows.isEmpty()) throw new ListingException(MarketplaceErrorCode.DRAFT_NOT_FOUND);
        String old = (String) rows.getFirst().get("listing_status");
        if (Set.of("AI_REVIEW", "PENDING_REVIEW", "PUBLISHED").contains(old))
            return Map.of("id", id, "listingStatus", old);
        if (!Set.of("DRAFT", "NEEDS_REVISION").contains(old))
            throw new ListingException(MarketplaceErrorCode.SUBMISSION_NOT_ALLOWED);
        if (repository.countImages(Integer.class, id) < 1)
            throw new ListingException(MarketplaceErrorCode.PHOTO_REQUIRED);
        repository.markAiReview(id);
        enqueue(id, owner);
        repository.insertProductStatusHistory(id, old, owner);
        return Map.of("id", id, "listingStatus", "AI_REVIEW");
    }

    public void enqueue(UUID id, UUID owner) {
        repository.advanceSubmittedRevision(id);
        var snapshot = snapshot(id);
        events.publish("listing.submitted.v1", id, ((Number) snapshot.get("revision")).longValue(), snapshot);
    }

    public Map<String, Object> snapshot(UUID id) {
        var item = repository.findReviewSnapshot(id);
        item.put("serialToken", serialToken(Objects.toString(item.remove("serial_number_private"), "")));

        for (var old : repository.findUnfingerprintedImages(id)) {
            UUID media = (UUID) old.get("id");
            if (!("product-public/" + media + ".png").equals(old.get("storage_key"))) continue;
            try {
                var image = ImageIO.read(mediaRoot.resolve(media + ".png").toFile());
                if (image == null) continue;
                var fp = ImageFingerprint.of(image);
                repository.saveFingerprint(fp.sha256(), fp.dhash(), fp.signature(), media);
            } catch (java.io.IOException ignored) { /* Missing historical assets force human review downstream. */ }
        }
        item.put("images", repository.findFingerprints(id));
        return item;
    }

    public void stateChanged(UUID id) {
        var snapshot = snapshot(id);
        events.publish("listing.state.v1", id, ((Number) snapshot.get("revision")).longValue(), snapshot);
    }

    private String serialToken(String serial) {
        String normalized = java.text.Normalizer.normalize(serial, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (normalized.isEmpty()) return "";
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return "v1:" + HexFormat.of().formatHex(mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

