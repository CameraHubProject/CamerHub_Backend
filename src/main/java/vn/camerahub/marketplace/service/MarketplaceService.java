package vn.camerahub.marketplace.service;

import vn.camerahub.marketplace.exception.*;

import vn.camerahub.marketplace.util.CameraBrands;
import vn.camerahub.marketplace.service.ListingSubmission;
import vn.camerahub.marketplace.repository.ProductRepository;
import vn.camerahub.marketplace.dto.MarketplaceDtos;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@lombok.RequiredArgsConstructor
public class MarketplaceService {
    private final ProductRepository repository;
    private final ObjectMapper json;
    private final ListingSubmission moderation;
    private final vn.camerahub.shared.InternalClient client;

    public List<Map<String, Object>> products() {
        return repository.findPublished();
    }

    public Map<String, Object> detail(UUID id) {
        var rows = repository.findPublicDetail(id);
        if (rows.isEmpty()) throw new ListingException(MarketplaceErrorCode.PRODUCT_NOT_AVAILABLE);
        var result = rows.getFirst();

        return result;
    }

    public List<Map<String, Object>> models() {
        return repository.findCameraModels();
    }

    public List<Map<String, Object>> brands() {
        var rows = repository.findBrands();
        rows.sort(Comparator.<Map<String, Object>>comparingInt(row -> CameraBrands.rank((String) row.get("name")))
                .thenComparing(row -> (String) row.get("name"), String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    public List<Map<String, Object>> listings(UUID user) {
        return repository.findSellerListings(user);
    }

    public Map<String, Object> submit(UUID user, UUID id) {
        return moderation.submit(user, id);
    }

    public Map<String, Object> draft(UUID user, UUID id) {
        var rows = repository.findEditableDraft(id, user);
        if (rows.isEmpty()) throw new ListingException(MarketplaceErrorCode.EDITABLE_DRAFT_NOT_FOUND);
        var item = rows.getFirst();
        item.put("modelId", repository.findModelId(UUID.class, id));
        item.put("serialNumber", repository.findPrivateSerial(String.class, id));
        item.put("media", repository.findDraftMedia(id));
        return item;
    }

    @Transactional
    public Map<String, Object> saveDraft(UUID user, UUID id, MarketplaceDtos.Listing input) {
        var rows = repository.lockEditableDraft(id, user);
        if (rows.isEmpty()) throw new ListingException(MarketplaceErrorCode.LISTING_NOT_EDITABLE);
        UUID model = resolveListingModel(input);
        if (!model.equals(rows.getFirst().get("model_id")) &&
                (!"NOT_INSPECTED".equals(rows.getFirst().get("inspection_badge")) || repository.countInspectionChecks(Integer.class, id) > 0))
            throw new ListingException(MarketplaceErrorCode.INSPECTED_MODEL_LOCKED);
        repository.saveDraft(model, input.title().strip(), money(input.priceVnd()), input.shutterCount(), input.description().strip(), input.conditionNotes().strip(), input.accessories(), input.serialNumber(), encode(Map.of("purposes", input.purposes())), id);
        return Map.of("id", id, "listingStatus", rows.getFirst().get("listing_status"));
    }

    public List<Map<String, Object>> publicSellerProducts(UUID seller, String status, int limit, int offset) {
        return repository.findPublicSellerListings(seller, status, limit, offset);
    }

    @Transactional
    public Map<String, Object> listing(UUID user, MarketplaceDtos.Listing input) {
        moderation.ensureAccount(user);
        UUID model = resolveListingModel(input);
        long price = money(input.priceVnd());
        UUID id = UUID.randomUUID();
        repository.insertDraft(id, user, model, input.title().strip(), price, input.shutterCount(), input.description().strip(), input.conditionNotes().strip(), input.accessories(), input.serialNumber(), encode(Map.of("purposes", input.purposes())));
        repository.insertProductStatusHistory(id, user);
        return Map.of("id", id, "listingStatus", "DRAFT");
    }

    private UUID resolveListingModel(MarketplaceDtos.Listing input) {
        if (!input.isSelectionValid()) throw new ListingException(MarketplaceErrorCode.INVALID_BRAND_SELECTION);
        if (input.modelId() != null) {
            if (repository.countModel(Integer.class, input.modelId()) != 1)
                throw new ListingException(MarketplaceErrorCode.MODEL_NOT_FOUND);
            return input.modelId();
        }
        UUID brand = input.brandName() != null ? resolveBrand(input.brandName(), input.customBrandName()) : input.brandId();
        if (repository.countBrand(Integer.class, brand) != 1)
            throw new ListingException(MarketplaceErrorCode.BRAND_NOT_AVAILABLE);
        String type = input.deviceType() == null ? "BODY" : input.deviceType();
        return unspecifiedModel(brand, type);
    }

    private UUID resolveBrand(String selected, String custom) {
        repository.lockBrandCatalog();
        var available = repository.findBrandNames(String.class);
        String name = CameraBrands.resolve(selected, custom, available);
        return repository.upsertBrand(UUID.class, name);
    }

    private UUID unspecifiedModel(UUID brand, String type) {
        // The existing schema requires model_id. Reuse one explicitly unspecified record per
        // brand/type, rather than attributing the listing to an arbitrary real camera model.
        // This is ordinary listing data, with no schema migration or modification of old models.
        return repository.upsertUnspecifiedModel(UUID.class, brand, type);
    }

    @Transactional
    public Map<String, Object> changeListingBrand(UUID user, UUID id, MarketplaceDtos.BrandSelection input) {
        var rows = repository.lockBrandSelection(id, user);
        if (rows.isEmpty()) throw new ListingException(MarketplaceErrorCode.LISTING_NOT_FOUND);
        var current = rows.getFirst();
        String status = (String) current.get("listing_status");
        if (Set.of("AI_REVIEW", "PENDING_REVIEW", "RESERVED", "SOLD", "REJECTED").contains(status))
            throw new ListingException(MarketplaceErrorCode.BRAND_EDIT_NOT_ALLOWED);
        UUID brand = resolveBrand(input.brandName(), input.customBrandName());
        if (brand.equals(current.get("brand_id"))) return Map.of("id", id, "listingStatus", status);
        if (!"NOT_INSPECTED".equals(current.get("inspection_badge")) || repository.countExistingInspectionChecks(Integer.class, id) > 0)
            throw new ListingException(MarketplaceErrorCode.INSPECTED_BRAND_LOCKED);
        UUID model = unspecifiedModel(brand, (String) current.get("device_type"));
        repository.changeBrandAndRequeue(model, id, user);
        if (Set.of("DRAFT", "NEEDS_REVISION").contains(status)) {
            repository.restoreDraftStatus(status, id);
            return Map.of("id", id, "listingStatus", status);
        }
        moderation.enqueue(id, user);
        return Map.of("id", id, "listingStatus", "AI_REVIEW");
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static long money(String value) {
        if (value == null || !value.matches("[1-9][0-9]{0,12}"))
            throw new ListingException(MarketplaceErrorCode.INVALID_PRICE);
        return Long.parseLong(value);
    }
}

