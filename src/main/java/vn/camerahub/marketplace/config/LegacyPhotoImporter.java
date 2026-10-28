package vn.camerahub.marketplace.config;

import java.nio.file.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import vn.camerahub.marketplace.repository.*;
import vn.camerahub.marketplace.service.ProductMediaService;

@Component
@lombok.RequiredArgsConstructor
public class LegacyPhotoImporter implements ApplicationRunner {
    private final ProductMediaRepository media;
    private final ProductMediaDataRepository contents;
    private final ProductMediaService service;
    @Value("${camerahub.media-root:storage/product-media}") private String directory;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path root = Path.of(directory).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) return;
        int imported = 0;
        for (var item : media.findAll()) {
            if (!("product-public/" + item.getId() + ".png").equals(item.getStorageKey()) || contents.existsById(item.getId())) continue;
            Path file = root.resolve(item.getId() + ".png");
            if (Files.isRegularFile(file) && Files.size(file) <= 20L * 1024 * 1024) {
                service.persist(item.getId(), Files.readAllBytes(file));imported++;
            }
        }
        LoggerFactory.getLogger(getClass()).info("Imported {} existing product photos into database storage", imported);
    }
}
