package vn.camerahub.marketplace.service;

import vn.camerahub.marketplace.exception.*;

import vn.camerahub.marketplace.service.ListingSubmission;
import vn.camerahub.marketplace.service.MarketplaceService;

import java.util.*;

import org.springframework.web.bind.annotation.*;
import vn.camerahub.shared.InternalClient;

@org.springframework.stereotype.Service
@lombok.RequiredArgsConstructor
public class PublicCatalogService {
    private final MarketplaceService service;
    private final ListingSubmission submissions;
    private final InternalClient client;

    public Object featured() {
        return Map.of("items", service.products());
    }

    public Object profile(UUID id) {
        submissions.ensureAccount(id);
        var data = new LinkedHashMap<String, Object>();
        var account = client.get("identity", "/internal/accounts/" + id);
        var stats = client.get("commerce", "/internal/sellers/" + id + "/stats");
        data.put("id", id);
        data.put("fullName", account.path("fullName").asText());
        data.put("joinedAt", account.path("joinedAt").asText());
        data.put("verified", false);
        data.put("sellingCount", service.publicSellerProducts(id, "PUBLISHED", Integer.MAX_VALUE, 0).size());
        data.put("soldCount", service.publicSellerProducts(id, "SOLD", Integer.MAX_VALUE, 0).size());
        data.put("reviewCount", stats.path("reviewCount").asLong());
        data.put("averageRating", stats.path("averageRating").isNull() ? null : stats.path("averageRating").asDouble());
        return data;
    }

    public Object listings(UUID id, String status, int page) {
        submissions.ensureAccount(id);
        if (!Set.of("PUBLISHED", "SOLD").contains(status) || page < 0 || page > 100000)
            throw new ListingException(MarketplaceErrorCode.INVALID_FILTER);
        var all = service.publicSellerProducts(id, status, Integer.MAX_VALUE, 0);
        return Map.of("items", all.stream().skip(page * 12L).limit(12).toList(), "total", all.size(), "page", page, "pageSize", 12);
    }
}

