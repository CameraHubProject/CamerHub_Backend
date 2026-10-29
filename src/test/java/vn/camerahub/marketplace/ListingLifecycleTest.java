package vn.camerahub.marketplace;

import java.util.*;
import org.junit.jupiter.api.*;
import vn.camerahub.marketplace.dto.MarketplaceDtos.ListingReason;
import vn.camerahub.marketplace.repository.ProductRepository;
import vn.camerahub.marketplace.service.*;
import vn.camerahub.shared.exception.AppException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListingLifecycleTest {
    ProductRepository repository = mock(ProductRepository.class);
    ListingSubmission submissions = mock(ListingSubmission.class);
    ListingLifecycleService service = new ListingLifecycleService(repository, submissions);
    UUID owner = UUID.randomUUID(), id = UUID.randomUUID();

    void status(String status) {
        when(repository.lockListingLifecycle(id, owner)).thenReturn(List.of(Map.of("listing_status", status)));
    }

    @Test void hideRequiresReasonAndSavesTrimmedHistory() {
        status("PUBLISHED");
        assertThat(service.withdraw(owner,id,new ListingReason("  Tạm ngừng bán  "),false))
                .containsEntry("listingStatus","HIDDEN").containsEntry("deleted",false);
        verify(repository).withdrawListing("Tạm ngừng bán",false,id,owner);
        verify(repository).recordListingAction(id,"PUBLISHED","HIDDEN",owner,"Ẩn tin: Tạm ngừng bán");
        verify(submissions).stateChanged(id);
    }

    @Test void deleteHiddenListingRemovesItFromOwnerInventoryAndKeepsReason() {
        status("HIDDEN");
        assertThat(service.withdraw(owner,id,new ListingReason("Đăng nhầm"),true)).containsEntry("deleted",true);
        verify(repository).withdrawListing("Đăng nhầm",true,id,owner);
        verify(repository).recordListingAction(id,"HIDDEN","HIDDEN",owner,"Xóa tin: Đăng nhầm");
    }

    @Test void whitespaceAndOversizedReasonsNeverChangeData() {
        for (String reason : Arrays.asList(null,"", "   ","x".repeat(2001))) {
            assertThatThrownBy(() -> service.withdraw(owner,id,new ListingReason(reason),true))
                    .isInstanceOfSatisfying(AppException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
        }
        verifyNoInteractions(repository,submissions);
    }

    @Test void anotherOwnerOrDeletedListingCannotBeModified() {
        assertThatThrownBy(() -> service.withdraw(UUID.randomUUID(),id,new ListingReason("Không dùng"),true))
                .isInstanceOfSatisfying(AppException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        verify(repository,never()).withdrawListing(any(),any(),any(),any());
        verifyNoInteractions(submissions);
    }

    @Test void reservedAndSoldListingsAreProtectedForBothActions() {
        for(String status : List.of("RESERVED","SOLD")) {
            status(status);
            for(boolean delete : List.of(false,true)) {
                assertThatThrownBy(() -> service.withdraw(owner,id,new ListingReason("Không dùng"),delete))
                        .isInstanceOfSatisfying(AppException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
            }
        }
        verify(repository,never()).withdrawListing(any(),any(),any(),any());
    }

    @Test void existingOrderOrHoldPreventsWithdrawalEvenIfStatusIsStale() {
        status("PUBLISHED");
        when(repository.hasListingCommitments(Boolean.class,id,id)).thenReturn(true);
        assertThatThrownBy(() -> service.withdraw(owner,id,new ListingReason("Tạm ngừng"),false))
                .isInstanceOfSatisfying(AppException.class,e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        verify(repository,never()).withdrawListing(any(),any(),any(),any());
    }

    @Test void hidingAgainIsIdempotent() {
        status("HIDDEN");
        service.withdraw(owner,id,new ListingReason("Tạm ngừng"),false);
        verify(repository,never()).withdrawListing(any(),any(),any(),any());
        verifyNoInteractions(submissions);
    }
}
