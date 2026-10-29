package vn.camerahub.marketplace;
import java.util.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import vn.camerahub.marketplace.service.ProductMediaService;
import vn.camerahub.marketplace.repository.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class ListingMediaEditingTest {
    ProductMediaRepository repository=mock(ProductMediaRepository.class);
    ProductMediaDataRepository contents=mock(ProductMediaDataRepository.class);
    ProductMediaService service=new ProductMediaService(repository,contents,"storage/product-media",mock(vn.camerahub.marketplace.service.ListingSubmission.class));
    UUID id=UUID.randomUUID(), user=UUID.randomUUID(), media=UUID.randomUUID();
    java.security.Principal principal=()->user.toString();
    @Test void ownerCanRemovePublishedPhotoAndListingNeedsReview() {
        when(repository.findProducts2(id,user)).thenReturn(List.of(Map.of("id",id)));
        when(repository.deleteProductMedia(media,id)).thenReturn(1);
        service.remove(principal,id,media);
        verify(repository).markMediaEdited(id,user);
    }
    @Test void ownerCanUploadPublishedPhotoAndListingNeedsReview() throws Exception {
        when(repository.findProducts(id,user)).thenReturn(List.of(Map.of("id",id)));
        when(repository.findProductMedia2(Integer.class,id)).thenReturn(0);
        var out=new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",out);
        service.upload(principal,id,new MockMultipartFile("file","image.png","image/png",out.toByteArray()));
        verify(repository).markMediaEdited(id,user); verify(contents).saveAndFlush(any());
    }
    @Test void otherUserCannotRemoveOrUploadAndNoStatusIsChanged() throws Exception {
        assertThatThrownBy(()->service.remove(principal,id,media)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->service.upload(principal,id,new MockMultipartFile("file",new byte[]{1}))).isInstanceOf(RuntimeException.class);
        verify(repository,never()).deleteProductMedia(any(),any()); verify(repository,never()).markMediaEdited(any(),any());
    }
}
