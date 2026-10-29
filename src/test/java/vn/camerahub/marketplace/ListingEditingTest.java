package vn.camerahub.marketplace;
import java.util.*;
import org.junit.jupiter.api.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.camerahub.marketplace.service.*;
import vn.camerahub.marketplace.entity.Product;
import vn.camerahub.marketplace.repository.ProductRepository;
import vn.camerahub.marketplace.dto.MarketplaceDtos.Listing;
import vn.camerahub.shared.exception.AppException;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class ListingEditingTest {
    ProductRepository repository = mock(ProductRepository.class);
    MarketplaceService service = new MarketplaceService(repository, new ObjectMapper(), mock(ListingSubmission.class), null);
    UUID owner=UUID.randomUUID(), id=UUID.randomUUID(), model=UUID.randomUUID();
    Listing input = new Listing(model,"Title","100000",null,"Description","Condition","Accessories","Serial",List.of());
    @BeforeEach void setup() {
        var item=new Product(); item.setId(id); item.setSellerId(owner);
        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(repository.lockEditableDraft(id,owner)).thenReturn(List.of(Map.of("listing_status","PUBLISHED","model_id",model,"inspection_badge","NOT_INSPECTED")));
        when(repository.countModel(Integer.class,model)).thenReturn(1);
    }
    @Test void authorCanSavePublishedContentAndRequeueForReview() {
        assertThat(service.saveDraft(owner,id,input)).containsEntry("listingStatus","NEEDS_REVISION");
        verify(repository).saveDraft(model,"Title",100000L,null,"Description","Condition","Accessories","Serial","{\"purposes\":[]}",id);
        verify(repository).restoreDraftStatus("NEEDS_REVISION",id);
    }
    @Test void authorCanChangeBodyToBodyAndLensKit() {
        UUID brand=UUID.randomUUID(), kitModel=UUID.randomUUID();
        var kit=new Listing(null,"Title","100000",null,"Description","Condition",null,null,List.of(),brand,"BODY_LENS");
        try(var validators=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            assertThat(validators.getValidator().validate(kit)).isEmpty();
        }
        when(repository.countBrand(Integer.class,brand)).thenReturn(1);
        when(repository.countInspectionChecks(Integer.class,id)).thenReturn(0);
        when(repository.upsertUnspecifiedModel(UUID.class,brand,"BODY_LENS")).thenReturn(kitModel);
        service.saveDraft(owner,id,kit);
        verify(repository).saveDraft(kitModel,"Title",100000L,null,"Description","Condition",null,null,"{\"purposes\":[]}",id);
    }
    @Test void otherAuthorIsForbiddenEvenWhenTheyKnowId() {
        UUID other=UUID.randomUUID();
        assertThatThrownBy(()->service.draft(other,id)).isInstanceOfSatisfying(AppException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(403));
        assertThatThrownBy(()->service.saveDraft(other,id,input)).isInstanceOfSatisfying(AppException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(403));
        verify(repository,never()).lockEditableDraft(any(),any());
    }
    @Test void hiddenListingsStayHiddenWhenSavedAndReviewingOrRejectedListingsBecomeEditableDrafts() {
        for (String status : List.of("HIDDEN","REJECTED","AI_REVIEW","PENDING_REVIEW")) {
            when(repository.lockEditableDraft(id,owner)).thenReturn(List.of(Map.of("listing_status",status,"model_id",model,"inspection_badge","NOT_INSPECTED")));
            assertThat(service.saveDraft(owner,id,input)).containsEntry("listingStatus","HIDDEN".equals(status) ? "HIDDEN" : "NEEDS_REVISION");
        }
    }
    @Test void missingListingIs404() {
        assertThatThrownBy(()->service.draft(owner,UUID.randomUUID())).isInstanceOfSatisfying(AppException.class,e->assertThat(e.getStatusCode().value()).isEqualTo(404));
    }
    @Test void invalidPriceIsRejectedBeforeSaving() {
        var bad=new Listing(model,"Title","0",null,"Description","Condition",null,null,List.of());
        assertThatThrownBy(()->service.saveDraft(owner,id,bad)).isInstanceOf(AppException.class);
        verify(repository,never()).restoreDraftStatus(any(),any());
    }
}
