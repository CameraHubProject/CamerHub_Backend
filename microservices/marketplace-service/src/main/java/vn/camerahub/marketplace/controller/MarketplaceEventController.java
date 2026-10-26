package vn.camerahub.marketplace.controller;

import vn.camerahub.marketplace.service.MarketplaceEventService;

import java.util.*;

import org.springframework.web.bind.annotation.*;
import vn.camerahub.micro.*;

import vn.camerahub.marketplace.service.MarketplaceEventService.*;

@RestController
@lombok.RequiredArgsConstructor
public class MarketplaceEventController {
    private final MarketplaceEventService service;

    @PostMapping("/internal/events")
    public Object receive(@RequestBody IntegrationEvents.Event event) {
        return service.receive(event);
    }

    @GetMapping("/internal/index-snapshots")
    public Object snapshots(@RequestParam(defaultValue = "0") int offset) {
        return service.snapshots(offset);
    }
}

