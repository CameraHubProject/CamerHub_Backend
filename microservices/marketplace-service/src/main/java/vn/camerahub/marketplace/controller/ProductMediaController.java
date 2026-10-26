package vn.camerahub.marketplace.controller;

import vn.camerahub.marketplace.service.ProductMediaService;

import java.nio.file.*;
import java.security.Principal;
import java.util.*;
import java.io.IOException;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import vn.camerahub.marketplace.service.ProductMediaService.*;

@RestController
@lombok.RequiredArgsConstructor
public class ProductMediaController {
    private final ProductMediaService service;

    @PostMapping("/api/marketplace/listings/{id}/media")

    @vn.camerahub.micro.dto.SuccessMessage("Đã tải ảnh lên tin đăng.")
    public Map<String, Object> upload(Principal principal, @PathVariable UUID id, @RequestParam("file") MultipartFile file) throws IOException {
        return service.upload(principal, id, file);
    }

    @GetMapping("/api/public/product-media/{id}")
    public ResponseEntity<FileSystemResource> image(Authentication authentication, @PathVariable UUID id) {
        return service.image(authentication, id);
    }

    @GetMapping("/internal/media/{id}")
    public ResponseEntity<FileSystemResource> internalImage(@PathVariable UUID id) {
        return service.internalImage(id);
    }

    @PostMapping("/api/marketplace/listings/{id}/media/{media}/remove")

    public vn.camerahub.micro.dto.ApiResponse<Void> remove(Principal principal, @PathVariable UUID id, @PathVariable UUID media) {
        service.remove(principal, id, media);

        return vn.camerahub.micro.dto.ApiResponse.success(200, "Đã xóa ảnh khỏi bản nháp.", null);
    }
}

