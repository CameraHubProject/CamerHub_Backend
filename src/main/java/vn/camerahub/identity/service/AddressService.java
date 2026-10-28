package vn.camerahub.identity.service;

import vn.camerahub.identity.exception.*;

import java.security.Principal;
import java.time.Clock;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.camerahub.identity.entity.UserAddress;
import vn.camerahub.identity.repository.*;
import vn.camerahub.identity.mapper.UserAddressMapper;
import vn.camerahub.identity.dto.AddressDtos.Address;
import vn.camerahub.identity.dto.response.UserAddressResponse;
import vn.camerahub.shared.exception.ResourceNotFoundException;

@Service
@lombok.RequiredArgsConstructor
public class AddressService {
    private final UserAddressJpaRepository repository;
    private final AccountRepository accounts;
    private final UserAddressMapper mapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<UserAddressResponse> list(Principal user) {
        return repository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(UUID.fromString(user.getName())).stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public Map<String, UUID> save(Principal user, Address request) {
        UUID id = UUID.fromString(user.getName());
        accounts.lockById(id).orElseThrow(() -> new AccountException(IdentityErrorCode.USER_NOT_FOUND));
        if (request.isDefault()) repository.clearDefaults(id);
        var address = new UserAddress();
        address.setId(UUID.randomUUID());
        address.setUserId(id);
        address.setRecipientName(request.recipientName().strip());
        address.setPhone(request.phone());
        address.setProvince(request.province().strip());
        address.setStreetAddress(request.streetAddress().strip());
        address.setIsDefault(request.isDefault());
        address.setCreatedAt(clock.instant());
        repository.saveAndFlush(address);
        return Map.of("id", address.getId());
    }
}
