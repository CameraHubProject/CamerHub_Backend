package vn.camerahub.marketplace.controller;

import vn.camerahub.marketplace.service.PublicCatalogService;

import java.util.*;

import org.springframework.web.bind.annotation.*;

import vn.camerahub.marketplace.service.PublicCatalogService.*;

@RestController
@lombok.RequiredArgsConstructor
public class PublicCatalogController {
    private final PublicCatalogService service;

    @GetMapping("/api/public/featured-products")
    public Object featured() {
        return service.featured();
    }

    @GetMapping("/api/public/sellers/{id}")
    public Object profile(@PathVariable UUID id) {
        return service.profile(id);
    }

    @GetMapping("/api/public/sellers/{id}/listings")
    public Object listings(@PathVariable UUID id, @RequestParam(defaultValue = "PUBLISHED") String status, @RequestParam(defaultValue = "0") int page) {
        return service.listings(id, status, page);
    }
}

