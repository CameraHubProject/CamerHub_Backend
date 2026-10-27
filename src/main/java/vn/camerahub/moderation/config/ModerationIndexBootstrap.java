package vn.camerahub.moderation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.camerahub.moderation.repository.ModerationRepository;
import vn.camerahub.moderation.service.ModerationService;

/** Enabled explicitly for a fresh cloud installation, after historical imports have completed. */
@Component
@ConditionalOnProperty(name = "camerahub.moderation.bootstrap-on-start", havingValue = "true")
public class ModerationIndexBootstrap {
    private static final Logger log = LoggerFactory.getLogger(ModerationIndexBootstrap.class);
    private final ModerationRepository repository;
    private final ModerationService service;

    public ModerationIndexBootstrap(ModerationRepository repository, ModerationService service) {
        this.repository = repository;
        this.service = service;
    }

    @Scheduled(initialDelayString = "${camerahub.moderation.bootstrap-delay-ms:10000}", fixedDelay = 30000)
    public void initialize() {
        if (Boolean.TRUE.equals(repository.isIndexReady(Boolean.class))) return;
        try {
            service.bootstrap();
            log.info("Duplicate listing index is ready");
        } catch (RuntimeException exception) {
            // Do not log remote responses, credentials, or listing contents.
            log.warn("Duplicate listing index is not ready; bootstrap will retry in 30 seconds");
        }
    }
}
