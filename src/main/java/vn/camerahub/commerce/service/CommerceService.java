package vn.camerahub.commerce.service;

import vn.camerahub.commerce.exception.*;

import vn.camerahub.commerce.repository.OrderRepository;
import vn.camerahub.commerce.dto.CommerceDtos;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@lombok.RequiredArgsConstructor
public class CommerceService {
    private final OrderRepository repository;
    private final ObjectMapper json;

    public List<Map<String, Object>> orders(UUID user) {
        return repository.findDatabase(user, user);
    }

    private List<Map<String, Object>> batchOrders(UUID user, UUID batch) {
        return repository.findDatabase2(user, batch);
    }

    public Map<String, Object> order(UUID user, UUID id) {
        var rows = repository.findDatabase3(id, user, user);
        if (rows.isEmpty()) throw new OrderException(CommerceErrorCode.ORDER_NOT_FOUND);
        var result = rows.getFirst();
        result.put("address", parse((String) result.remove("addressJson")));
        result.put("payments", repository.findPaymentRecords(id));
        var reviews = repository.findSellerReviews(id);
        result.put("sellerReview", reviews.isEmpty() ? null : reviews.getFirst());
        result.put("canReviewSeller", user.toString().equals(result.get("buyerId").toString()) && "COMPLETED".equals(result.get("status")) && reviews.isEmpty());
        return result;
    }

    @Transactional
    public Map<String, Object> pay(UUID user, UUID id, String stage) {
        var rows = repository.lockBuyerOrder(id, user);
        if (rows.isEmpty()) throw new OrderException(CommerceErrorCode.ORDER_NOT_FOUND);
        var order = rows.getFirst();
        boolean deposit = "DEPOSIT".equals(stage);
        if (!deposit && !"BALANCE_AND_SHIPPING".equals(stage))
            throw new OrderException(CommerceErrorCode.INVALID_PAYMENT_STAGE);
        // A retry for the same stage never creates a second successful payment.
        if (repository.findPaymentRecords2(Integer.class, id, stage) > 0) return order(user, id);
        String expected = deposit ? "AWAITING_DEPOSIT" : "AWAITING_BALANCE";
        if (!expected.equals(order.status()) || order.expired())
            throw new OrderException(CommerceErrorCode.PAYMENT_STAGE_EXPIRED);
        long amount = deposit ? order.depositAmountVnd() :
                Math.addExact(order.balanceAmountVnd(), order.shippingFeeVnd());
        repository.insertPaymentRecords(id, amount, id + ":" + stage, stage);
        repository.updateOrderDeposits(deposit ? "PAID" : "APPLIED", id);
        repository.updateOrders(deposit ? "AWAITING_BALANCE" : "READY_TO_SHIP", deposit, id);
        return order(user, id);
    }

    public List<Map<String, Object>> buybacks(UUID user) {
        return repository.findBuybackRequests(user);
    }

    public Map<String, Object> buyback(UUID user, CommerceDtos.Buyback input) {
        UUID id = UUID.randomUUID();
        repository.insertBuybackRequests(id, user, input.model().strip(), input.conditionNotes().strip(), input.expectedPriceVnd() == null || input.expectedPriceVnd().isBlank() ? null : money(input.expectedPriceVnd()));
        return Map.of("id", id, "status", "PENDING");
    }

    private Map<String, Object> parse(String value) {
        try {
            return json.readValue(value, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Stored JSON is invalid", e);
        }
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static long money(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,12}"))
            throw new OrderException(CommerceErrorCode.INVALID_PRICE);
        return Long.parseLong(value);
    }

}

