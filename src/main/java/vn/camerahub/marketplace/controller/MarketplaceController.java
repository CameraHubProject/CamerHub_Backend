package vn.camerahub.marketplace.controller;

import vn.camerahub.marketplace.service.MarketplaceService;
import vn.camerahub.marketplace.dto.MarketplaceDtos.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.security.Principal;
import java.util.*;

import org.springframework.web.bind.annotation.*;

@RestController
@lombok.RequiredArgsConstructor
public class MarketplaceController {
    private final MarketplaceService service;

    private UUID user(Principal principal) {
        return UUID.fromString(principal.getName());
    }

    @GetMapping("/api/public/products")
    public Map<String, Object> products() {
        return Map.of("items", service.products());
    }

    @GetMapping("/api/public/products/{id}")
    public Map<String, Object> product(@PathVariable UUID id) {
        return service.detail(id);
    }

    @GetMapping("/api/public/models")
    public Object models() {
        return service.models();
    }

    @GetMapping("/api/public/brands")
    public Object brands() {
        return service.brands();
    }

    @GetMapping("/api/marketplace/listings")
    public Object listings(Principal p) {
        return service.listings(user(p));
    }

    @PostMapping("/api/marketplace/listings")
    @vn.camerahub.shared.dto.SuccessMessage("Đã lưu bản nháp tin đăng.")
    public Object listing(Principal p, @Valid @RequestBody Listing input) {
        return service.listing(user(p), input);
    }

    @PostMapping("/api/marketplace/listings/{id}/submit")
    @vn.camerahub.shared.dto.SuccessMessage("Đã gửi tin. AI đang kiểm tra nội dung và ảnh; tin đạt sẽ được đăng tự động. Trường hợp chưa rõ sẽ chuyển Admin.")
    public Object submit(Principal p, @PathVariable UUID id) {
        return service.submit(user(p), id);
    }

    @GetMapping("/api/marketplace/listings/{id}/draft")
    public Object draft(Principal p, @PathVariable UUID id) {
        return service.draft(user(p), id);
    }

    @PostMapping("/api/marketplace/listings/{id}/draft")
    @vn.camerahub.shared.dto.SuccessMessage("Đã cập nhật bản nháp.")
    public Object saveDraft(Principal p, @PathVariable UUID id, @Valid @RequestBody Listing input) {
        return service.saveDraft(user(p), id, input);
    }

    @PostMapping("/api/marketplace/listings/{id}/brand")
    @vn.camerahub.shared.dto.SuccessMessage("Đã cập nhật hãng máy của tin đăng.")
    public Object listingBrand(Principal p, @PathVariable UUID id, @Valid @RequestBody BrandSelection input) {
        return service.changeListingBrand(user(p), id, input);
    }

}

