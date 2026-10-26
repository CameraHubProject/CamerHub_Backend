package vn.camerahub.moderation.config;

import org.junit.jupiter.api.Test;
import vn.camerahub.moderation.repository.ModerationRepository;
import vn.camerahub.moderation.service.ModerationService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class ModerationIndexBootstrapTest {
    private final ModerationRepository repository = mock(ModerationRepository.class);
    private final ModerationService service = mock(ModerationService.class);
    private final ModerationIndexBootstrap bootstrap = new ModerationIndexBootstrap(repository, service);

    @Test
    void readyIndexIsNotRebuilt() {
        when(repository.isIndexReady(Boolean.class)).thenReturn(true);
        bootstrap.initialize();
        verifyNoInteractions(service);
    }

    @Test
    void unavailableMarketplaceIsRetriedWithoutEnablingWorkers() {
        when(repository.isIndexReady(Boolean.class)).thenReturn(false);
        when(service.bootstrap()).thenThrow(new IllegalStateException("not ready")).thenReturn(null);
        assertDoesNotThrow(bootstrap::initialize);
        bootstrap.initialize();
        verify(service, times(2)).bootstrap();
        verify(repository, never()).markIndexReady();
    }
}
