package vn.camerahub.marketplace.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.camerahub.marketplace.dto.MarketplaceDtos.ListingReason;
import vn.camerahub.marketplace.exception.*;
import vn.camerahub.marketplace.repository.ProductRepository;

@Service
@lombok.RequiredArgsConstructor
public class ListingLifecycleService {
    private final ProductRepository repository;
    private final ListingSubmission submissions;

    @Transactional
    public Map<String, Object> withdraw(UUID owner, UUID id, ListingReason input, boolean delete) {
        String reason = input == null || input.reason() == null ? "" : input.reason().strip();
        if (reason.isEmpty() || reason.length() > 2000)
            throw new ListingException(MarketplaceErrorCode.LISTING_REASON_REQUIRED);
        var rows = repository.lockListingLifecycle(id, owner);
        if (rows.isEmpty()) throw new ListingException(MarketplaceErrorCode.LISTING_NOT_FOUND);
        String old = (String) rows.getFirst().get("listing_status");
        if (Set.of("RESERVED", "SOLD").contains(old)
                || Boolean.TRUE.equals(repository.hasListingCommitments(Boolean.class, id, id)))
            throw new ListingException(MarketplaceErrorCode.LISTING_WITHDRAWAL_NOT_ALLOWED);
        if (!delete && "HIDDEN".equals(old)) return Map.of("id", id, "listingStatus", "HIDDEN", "deleted", false);
        repository.withdrawListing(reason, delete, id, owner);
        repository.recordListingAction(id, old, "HIDDEN", owner, (delete ? "Xóa tin: " : "Ẩn tin: ") + reason);
        submissions.stateChanged(id);
        return Map.of("id", id, "listingStatus", "HIDDEN", "deleted", delete);
    }
}
