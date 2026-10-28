package vn.camerahub.marketplace.service;

import vn.camerahub.marketplace.exception.*;

import vn.camerahub.marketplace.service.ListingSubmission;
import vn.camerahub.marketplace.service.MarketplaceService;
import vn.camerahub.marketplace.repository.ReservationRepository;
import vn.camerahub.marketplace.dto.ReservationDtos.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

import org.springframework.http.HttpStatus;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.camerahub.shared.*;

@org.springframework.stereotype.Service
@lombok.RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository repository;
    private final InternalClient client;
    private final IntegrationEvents events;
    private final ObjectMapper json;
    private final ListingSubmission submissions;

    @Transactional
    public Object reserve(@Valid Reserve input) {
        var ids = input.productIds().stream().distinct().sorted().toList();
        if (ids.size() != input.productIds().size())
            throw new ReservationException(MarketplaceErrorCode.DUPLICATE_PRODUCT);
        if (!"ACTIVE".equals(client.get("identity", "/internal/accounts/" + input.buyerId()).path("status").asText()))
            throw new ReservationException(MarketplaceErrorCode.ACCOUNT_INACTIVE);
        repository.lockRequest("reservation:" + input.requestId());
        var previous = repository.lockBatch(input.requestId());
        String encoded = events.encode(ids);
        if (!previous.isEmpty()) {
            var old = previous.getFirst();
            if (!input.buyerId().equals(old.get("buyer_id")) || !jsonEquivalent(encoded, old.get("product_ids").toString()))
                throw new ReservationException(MarketplaceErrorCode.RESERVATION_REQUEST_REUSED);
            if ("RELEASED".equals(old.get("state")))
                throw new ReservationException(MarketplaceErrorCode.RESERVATION_CANCELLED);
            return snapshots(input.requestId());
        }
        repository.insertBatch(input.requestId(), input.buyerId(), encoded);
        for (UUID id : ids) {
            var rows = repository.lockProduct(id);
            if (rows.isEmpty() || !"PUBLISHED".equals(rows.getFirst().listingStatus()))
                throw new ReservationException(MarketplaceErrorCode.OUT_OF_STOCK);
            var p = rows.getFirst();
            UUID seller = p.sellerId();
            if (seller.equals(input.buyerId()))
                throw new ReservationException(MarketplaceErrorCode.SELF_PURCHASE);
            if (!"ACTIVE".equals(client.get("identity", "/internal/accounts/" + seller).path("status").asText()))
                throw new ReservationException(MarketplaceErrorCode.SELLER_INACTIVE);
            UUID token = UUID.randomUUID();
            var snapshot = Map.of("id", id, "sellerId", seller, "title", p.title(), "priceVnd", Long.toString(p.priceVnd()), "conditionNotes", p.conditionNotes(), "reservationToken", token);
            if (repository.holdProduct(id, input.requestId(), input.buyerId(), token, events.encode(snapshot)) != 1)
                throw new ReservationException(MarketplaceErrorCode.PRODUCT_RESERVED);
            repository.markReserved(id);
            repository.insertProductStatusHistory(id, input.buyerId());
            submissions.stateChanged(id);
        }
        return snapshots(input.requestId());
    }

    @Transactional
    public Object confirm(UUID batch) {
        var state = client.get("commerce", "/internal/checkouts/" + batch).path("state").asText();
        if (!"PLACED".equals(state))
            throw new ReservationException(MarketplaceErrorCode.RESERVATION_UNCONFIRMED);
        repository.confirmReservations(batch);
        repository.confirmBatch(batch);
        return Map.of("accepted", true);
    }

    @Transactional
    public Object release(UUID batch) {
        var state = client.get("commerce", "/internal/checkouts/" + batch).path("state").asText();
        if (!Set.of("FAILED", "CANCELLED").contains(state))
            throw new ReservationException(MarketplaceErrorCode.CHECKOUT_CANCELLATION_UNCONFIRMED);
        for (UUID id : repository.lockBatchProducts(UUID.class, batch)) {
            repository.releaseBatchInventory(id);
            repository.releaseBatchProduct(id, batch);
            submissions.stateChanged(id);
        }
        repository.releaseBatch(batch);
        return Map.of("accepted", true);
    }

    private List<Object> snapshots(UUID batch) {
        return repository.findSnapshots(String.class, batch).stream().map(value -> {
            try {
                return json.readValue(value, Object.class);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).toList();
    }

    @Transactional
    public Object cancel(UUID batch, UUID product, Map<String, String> input) {
        UUID order = UUID.fromString(input.get("orderId"));
        var record = client.get("commerce", "/internal/orders/" + order);
        if (!"CANCELLED".equals(record.path("status").asText()) || !product.toString().equals(record.path("productId").asText()) || !batch.toString().equals(record.path("batchId").asText()))
            throw new ReservationException(MarketplaceErrorCode.ORDER_CANCELLATION_UNCONFIRMED);
        var rows = repository.lockReservation(product, batch);
        if (rows.isEmpty()) return Map.of("accepted", true);
        repository.releaseOrderInventory(product);
        repository.releaseOrderProduct(product, batch);
        submissions.stateChanged(product);
        return Map.of("accepted", true);
    }

    private boolean jsonEquivalent(String a, String b) {
        try {
            return json.readTree(a).equals(json.readTree(b));
        } catch (Exception e) {
            return false;
        }
    }
}

