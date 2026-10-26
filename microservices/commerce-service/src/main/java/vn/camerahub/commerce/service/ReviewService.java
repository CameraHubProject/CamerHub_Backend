package vn.camerahub.commerce.service;

import vn.camerahub.commerce.exception.*;

import vn.camerahub.commerce.repository.ReviewRepository;
import vn.camerahub.commerce.dto.ReviewDtos.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.security.Principal;
import java.util.*;

import org.springframework.http.HttpStatus;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import vn.camerahub.micro.InternalClient;

@org.springframework.stereotype.Service
@lombok.RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository repository;
    private final InternalClient client;

    public Object checkout(UUID id) {
        var rows = repository.findCheckoutBatches(id);
        if (rows.isEmpty()) throw new CheckoutException(CommerceErrorCode.CHECKOUT_NOT_FOUND);
        return rows.getFirst();
    }

    public Object order(UUID id) {
        var rows = repository.findOrders(id);
        if (rows.isEmpty()) throw new ReviewException(CommerceErrorCode.ORDER_NOT_FOUND);
        return rows.getFirst();
    }

    public Object stats(UUID id) {
        return repository.findSellerReviews(id);
    }

    public Object reviews(UUID id, int page) {
        if (page < 0 || page > 100000) throw new ReviewException(CommerceErrorCode.INVALID_PAGE);
        if (!"ACTIVE".equals(client.get("identity", "/internal/accounts/" + id).path("status").asText()))
            throw new ReviewException(CommerceErrorCode.SELLER_NOT_FOUND);
        var items = repository.findSellerReviews2(id, page * 12);
        for (var item : items) {
            var author = client.get("identity", "/internal/accounts/" + item.remove("buyer_id"));
            boolean active = "ACTIVE".equals(author.path("status").asText());
            item.put("authorId", active ? author.path("id").asText() : null);
            item.put("authorName", active ? author.path("fullName").asText() : "Thành viên CameraHub");
        }
        long total = repository.findSellerReviews3(Long.class, id);
        return Map.of("items", items, "total", total, "page", page, "pageSize", 12);
    }

    @Transactional
    public Object review(Principal p, UUID id, @Valid Review input) {
        UUID buyer = UUID.fromString(p.getName());
        var rows = repository.findOrders2(id, buyer);
        if (rows.isEmpty()) throw new ReviewException(CommerceErrorCode.ORDER_NOT_FOUND);
        if (!"COMPLETED".equals(rows.getFirst().get("order_status")) || buyer.equals(rows.getFirst().get("seller_id")))
            throw new ReviewException(CommerceErrorCode.REVIEW_NOT_ALLOWED);
        UUID review = UUID.randomUUID();
        repository.insertSellerReviews(review, id, input.rating(), input.comment().strip());
        return Map.of("id", review);
    }
}

