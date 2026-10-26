package vn.camerahub.marketplace.controller;

import vn.camerahub.marketplace.service.ReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.*;

import org.springframework.web.bind.annotation.*;
import vn.camerahub.micro.*;

import vn.camerahub.marketplace.dto.ReservationDtos.*;

@RestController
@lombok.RequiredArgsConstructor
public class ReservationController {
    private final ReservationService service;

    @PostMapping("/internal/reservations")
    public Object reserve(@Valid @RequestBody Reserve input) {
        return service.reserve(input);
    }

    @PostMapping("/internal/reservations/{batch}/confirm")
    public Object confirm(@PathVariable UUID batch) {
        return service.confirm(batch);
    }

    @PostMapping("/internal/reservations/{batch}/release")
    public Object release(@PathVariable UUID batch) {
        return service.release(batch);
    }

    @PostMapping("/internal/reservations/{batch}/{product}/cancel")
    public Object cancel(@PathVariable UUID batch, @PathVariable UUID product, @RequestBody Map<String, String> input) {
        return service.cancel(batch, product, input);
    }
}

