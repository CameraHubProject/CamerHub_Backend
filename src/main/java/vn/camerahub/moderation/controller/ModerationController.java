package vn.camerahub.moderation.controller;

import vn.camerahub.moderation.service.ModerationService;
import vn.camerahub.moderation.dto.ModerationDtos.*;

import java.security.Principal;
import java.util.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import vn.camerahub.shared.IntegrationEvents;

@RestController
@lombok.RequiredArgsConstructor
public class ModerationController {
    private final ModerationService service;

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/listings")
    public Object queue() {
        return service.queue();
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/admin/listings/{id}/review")
    @vn.camerahub.shared.dto.SuccessMessage("Đã lưu quyết định duyệt tin.")
    public Object review(Principal user, @PathVariable UUID id, @Valid @RequestBody Review input) {
        return service.review(UUID.fromString(user.getName()), id, input.revision(), input.approve(), input.note());
    }

}

