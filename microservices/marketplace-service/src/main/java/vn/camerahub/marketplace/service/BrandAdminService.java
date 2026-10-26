package vn.camerahub.marketplace.service;

import vn.camerahub.marketplace.exception.*;

import vn.camerahub.marketplace.repository.BrandAdminRepository;
import vn.camerahub.marketplace.util.CameraBrands;

import java.util.*;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@lombok.RequiredArgsConstructor
public class BrandAdminService {
    private final BrandAdminRepository repository;

    public List<Map<String, Object>> list() {
        var rows = repository.findCameraModels();
        rows.sort(Comparator.<Map<String, Object>>comparingInt(row -> CameraBrands.rank((String) row.get("name")))
                .thenComparing(row -> (String) row.get("name"), String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    @Transactional
    public Map<String, Object> save(UUID id, String input) {
        String name = CameraBrands.normalizeName(input);
        lockCatalog();
        if (id != null) requireBrand(id);
        var duplicates = repository.findBrands(UUID.class, name);
        for (UUID existing : duplicates)
            if (!existing.equals(id))
                throw new BrandException(MarketplaceErrorCode.BRAND_ALREADY_EXISTS);
        if (id == null) id = repository.findBrands2(UUID.class, name);
        else repository.updateBrands(name, id);
        return Map.of("id", id, "name", name);
    }

    @Transactional
    public void delete(UUID id) {
        lockCatalog();
        requireBrand(id);
        int changed = repository.deleteBrands(id);
        if (changed == 0)
            throw new BrandException(MarketplaceErrorCode.BRAND_IN_USE);
    }

    private void requireBrand(UUID id) {
        if (repository.findBrands3(UUID.class, id).isEmpty())
            throw new BrandException(MarketplaceErrorCode.BRAND_NOT_FOUND);
    }

    private void lockCatalog() {
        repository.lockDatabase();
    }
}

