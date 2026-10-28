package vn.camerahub.identity.controller;

import java.security.Principal;

import jakarta.validation.Valid;
import vn.camerahub.identity.service.AddressService;
import vn.camerahub.identity.entity.*;
import jakarta.validation.constraints.*;

import java.util.*;

import org.springframework.web.bind.annotation.*;

import vn.camerahub.identity.dto.AddressDtos.*;

@RestController
@lombok.RequiredArgsConstructor
public class AddressController {
    private final AddressService service;

    @GetMapping("/api/marketplace/addresses")
    public Object list(Principal user) {
        return service.list(user);
    }

    @PostMapping("/api/marketplace/addresses")
    @vn.camerahub.shared.dto.SuccessMessage("Đã lưu địa chỉ.")
    public Object save(Principal user, @Valid @RequestBody vn.camerahub.identity.dto.AddressDtos.Address input) {
        return service.save(user, input);
    }

}

