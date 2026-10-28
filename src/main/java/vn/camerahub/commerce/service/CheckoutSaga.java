package vn.camerahub.commerce.service;

import vn.camerahub.commerce.exception.*;

import vn.camerahub.commerce.repository.CheckoutRepository;
import vn.camerahub.commerce.service.CommerceService;
import vn.camerahub.commerce.dto.CommerceDtos;

import com.fasterxml.jackson.databind.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import org.springframework.http.HttpStatus;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import vn.camerahub.shared.*;

@Service
public class CheckoutSaga {
    private final CheckoutRepository repository;
    private final InternalClient client;
    private final ObjectMapper json;
    private final TransactionTemplate transactions;
    private final CommerceService orders;

    public CheckoutSaga(CheckoutRepository repository, InternalClient client, ObjectMapper json, PlatformTransactionManager manager, CommerceService orders) {
        this.repository = repository;
        this.client = client;
        this.json = json;
        this.transactions = new TransactionTemplate(manager);
        this.orders = orders;
    }

    public Object showrooms() {
        return repository.findShowrooms();
    }

    public Object checkout(UUID buyer, CommerceDtos.Checkout input) {
        var ids = input.productIds().stream().distinct().sorted().toList();
        if (ids.size() != input.productIds().size())
            throw new CheckoutException(CommerceErrorCode.DUPLICATE_PRODUCT);
        String request = encode(input), fingerprint = hash(encode(List.of(ids, input.delivery(), input.address())));
        transactions.executeWithoutResult(tx -> {
            repository.insertCheckoutBatches(input.requestId(), buyer, fingerprint, request);
            var old = repository.findCheckoutBatches(input.requestId());
            if (!buyer.equals(old.get("buyer_id")) || !fingerprint.equals(old.get("request_hash")))
                throw new CheckoutException(CommerceErrorCode.CHECKOUT_REQUEST_REUSED);
            if (Set.of("FAILED", "CANCELLED").contains(old.get("state")))
                throw new CheckoutException(CommerceErrorCode.CHECKOUT_CANCELLED);
        });
        attempt(input.requestId());
        String state = repository.findCheckoutBatches2(String.class, input.requestId());
        if (!"PLACED".equals(state))
            throw new CheckoutException(CommerceErrorCode.CHECKOUT_PROCESSING);
        return repository.findOrders(UUID.class, input.requestId()).stream().map(id -> orders.order(buyer, id)).toList();
    }

    private void attempt(UUID batch) {
        UUID lease = UUID.randomUUID();
        var rows = repository.findCheckoutBatches3(lease, batch);
        if (rows.isEmpty()) return;
        var row = rows.getFirst();
        UUID buyer = (UUID) row.get("buyer_id");
        try {
            var input = json.readValue(row.get("request").toString(), CommerceDtos.Checkout.class);
            String address = address(input);
            var reserved = client.post("marketplace", "/internal/reservations", Map.of("requestId", batch, "buyerId", buyer, "productIds", input.productIds()));
            transactions.executeWithoutResult(tx -> {
                var current = repository.findCheckoutBatches4(batch);
                if (!"PLACING".equals(current.get("state")) || !lease.equals(current.get("lease_token"))) return;
                for (var product : reserved) {
                    UUID order = UUID.randomUUID();
                    repository.insertOrders(order, UUID.fromString(product.path("id").asText()), buyer, UUID.fromString(product.path("sellerId").asText()), CommerceService.money(product.path("priceVnd").asText()), product.toString(), address, batch);
                    repository.insertOrderDeposits(order);
                }
                repository.updateCheckoutBatches(reserved.toString(), batch);
            });
            syncPlaced(batch);
        } catch (vn.camerahub.shared.exception.AppException e) {
            if (e.getStatusCode().is4xxClientError()) {
                repository.updateCheckoutBatches2(e.getMessage(), batch, lease);
                try {
                    client.post("marketplace", "/internal/reservations/" + batch + "/release", Map.of());
                } catch (Exception ignored) {
                }
            } else repository.updateCheckoutBatches3(batch, lease);
            throw e;
        } catch (Exception e) {
            repository.updateCheckoutBatches4(batch, lease);
            throw new CheckoutException(CommerceErrorCode.CHECKOUT_RECONCILING);
        }
    }

    private String address(CommerceDtos.Checkout input) {
        if (!"SHOWROOM".equals(input.delivery())) return encode(input.address());
        UUID room;
        try {
            room = UUID.fromString(input.address().streetAddress());
        } catch (Exception e) {
            throw new CheckoutException(CommerceErrorCode.SHOWROOM_REQUIRED);
        }
        var rooms = repository.findShowrooms2(room);
        if (rooms.isEmpty()) throw new CheckoutException(CommerceErrorCode.SHOWROOM_UNAVAILABLE);
        return encode(Map.of("delivery", "SHOWROOM", "recipientName", input.address().recipientName(), "phone", input.address().phone(), "showroom", rooms.getFirst()));
    }

    public void cancel(UUID buyer, UUID id) {
        transactions.executeWithoutResult(tx -> {
            var rows = repository.findOrders2(id, buyer);
            if (rows.isEmpty()) throw new CheckoutException(CommerceErrorCode.ORDER_NOT_FOUND);
            String status = (String) rows.getFirst().get("order_status");
            if ("CANCELLED".equals(status)) return;
            if (!"AWAITING_DEPOSIT".equals(status))
                throw new CheckoutException(CommerceErrorCode.ORDER_CANNOT_CANCEL);
            repository.updateOrders(id);
            repository.updateOrderDeposits(id);
        });
        syncCancelled(id);
    }

    private void syncPlaced(UUID batch) {
        try {
            client.post("marketplace", "/internal/reservations/" + batch + "/confirm", Map.of());
            repository.updateCheckoutBatches5(batch);
        } catch (
                Exception ignored) { /* Durable state is retried below; held inventory is never released on timeout. */ }
    }

    private void syncCancelled(UUID id) {
        var row = repository.findOrders3(id);
        if (row.get("checkout_batch_id") == null) return; // Legacy reservations are imported separately.
        try {
            client.post("marketplace", "/internal/reservations/" + row.get("checkout_batch_id") + "/" + row.get("product_id") + "/cancel", Map.of("orderId", id));
            repository.updateOrders2(id);
        } catch (Exception ignored) { /* Reconciliation preserves the cancellation if Marketplace is down. */ }
    }

    @Scheduled(fixedDelayString = "${camerahub.checkout.reconcile-ms:5000}")
    public void reconcile() {
        for (UUID batch : repository.findCheckoutBatches5(UUID.class))
            try {
                attempt(batch);
            } catch (Exception ignored) {
            }
        for (UUID batch : repository.findCheckoutBatches6(UUID.class)) syncPlaced(batch);
        for (UUID id : repository.findOrders4(UUID.class)) syncCancelled(id);
        for (UUID batch : repository.findCheckoutBatches7(UUID.class))
            try {
                client.post("marketplace", "/internal/reservations/" + batch + "/release", Map.of());
                repository.updateCheckoutBatches6(batch);
            } catch (Exception ignored) {
            }
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

