package vn.camerahub.commerce.controller;

import vn.camerahub.commerce.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.security.Principal;
import java.util.*;

import org.springframework.web.bind.annotation.*;

import vn.camerahub.commerce.dto.ReviewDtos.*;

@RestController
@lombok.RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;

    @GetMapping("/api/public/sellers/{id}/reviews")
    public Object reviews(@PathVariable UUID id, @RequestParam(defaultValue = "0") int page) {
        return service.reviews(id, page);
    }

    @PostMapping("/api/marketplace/orders/{id}/seller-review")
    @vn.camerahub.shared.dto.SuccessMessage("Đã gửi đánh giá người bán.")
    public Object review(Principal p, @PathVariable UUID id, @Valid @RequestBody Review input) {
        return service.review(p, id, input);
    }
}

