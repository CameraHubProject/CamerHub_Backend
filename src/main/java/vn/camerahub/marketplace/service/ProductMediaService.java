package vn.camerahub.marketplace.service;

import vn.camerahub.marketplace.exception.*;
import vn.camerahub.marketplace.util.ImageFingerprint;
import vn.camerahub.marketplace.repository.*;
import vn.camerahub.marketplace.entity.ProductMediaData;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

@org.springframework.stereotype.Service
public class ProductMediaService {
    private final ProductMediaRepository repository;
    private final ProductMediaDataRepository contents;
    private final Path root;
    private final ListingSubmission submissions;

    public ProductMediaService(ProductMediaRepository repository, ProductMediaDataRepository contents,
                               @org.springframework.beans.factory.annotation.Value("${camerahub.media-root:storage/product-media}") String root,
                               ListingSubmission submissions) {
        this.repository = repository;
        this.contents = contents;
        this.root = Path.of(root).toAbsolutePath().normalize();
        this.submissions = submissions;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upload(Principal principal, UUID id, MultipartFile file) throws IOException {
        UUID user = UUID.fromString(principal.getName());
        if (repository.findProducts(id, user).isEmpty()) throw new MediaException(MarketplaceErrorCode.PHOTO_UPLOAD_NOT_ALLOWED);
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) throw new MediaException(MarketplaceErrorCode.PHOTO_TOO_LARGE);
        java.awt.image.BufferedImage decoded;
        try (var stream = ImageIO.createImageInputStream(file.getInputStream())) {
            if (stream == null) throw new MediaException(MarketplaceErrorCode.INVALID_PHOTO);
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new MediaException(MarketplaceErrorCode.UNSUPPORTED_PHOTO_FORMAT);
            var reader = readers.next();
            try {
                if (!Set.of("JPEG", "PNG").contains(reader.getFormatName().toUpperCase(Locale.ROOT))) throw new MediaException(MarketplaceErrorCode.UNSUPPORTED_PHOTO_FORMAT);
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 24_000_000) throw new MediaException(MarketplaceErrorCode.PHOTO_DIMENSIONS_EXCEEDED);
                decoded = reader.read(0);
            } finally { reader.dispose(); }
        } catch (javax.imageio.IIOException error) { throw new MediaException(MarketplaceErrorCode.UNREADABLE_PHOTO); }
        var fingerprint = ImageFingerprint.of(decoded);
        var existing = repository.findProductMedia(UUID.class, id, fingerprint.sha256());
        if (!existing.isEmpty()) return result(existing.getFirst());
        if (repository.findProductMedia2(Integer.class, id) >= 8) throw new MediaException(MarketplaceErrorCode.PHOTO_LIMIT_EXCEEDED);
        var output = new ByteArrayOutputStream();
        if (!ImageIO.write(decoded, "png", output)) throw new IOException("PNG encoder unavailable");
        if (output.size() > 20 * 1024 * 1024) throw new MediaException(MarketplaceErrorCode.PHOTO_DIMENSIONS_EXCEEDED);
        UUID media = UUID.randomUUID();
        repository.insertProductMedia(media, id, "product-public/" + media + ".png", id, fingerprint.sha256(), fingerprint.dhash(), fingerprint.signature());
        persist(media, output.toByteArray());
        if (repository.markMediaEdited(id, user) > 0) submissions.stateChanged(id);
        return result(media);
    }

    public ResponseEntity<Resource> image(Authentication authentication, UUID id) {
        String owner = authentication == null ? "" : authentication.getName();
        boolean admin = authentication != null && authentication.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));
        var rows = repository.findProductMedia3(id, owner, admin);
        if (rows.isEmpty() || !("product-public/" + id + ".png").equals(rows.getFirst().get("storage_key"))) throw missing();
        return response(id);
    }

    public ResponseEntity<Resource> internalImage(UUID id) {
        var rows = repository.findProductMedia4(String.class, id);
        if (rows.isEmpty() || !("product-public/" + id + ".png").equals(rows.getFirst())) throw missing();
        return response(id);
    }

    private ResponseEntity<Resource> response(UUID id) {
        Resource resource = contents.findById(id).<Resource>map(row -> new ByteArrayResource(row.getContent())).orElseGet(() -> {
            Path file = root.resolve(id + ".png");
            if (!Files.isRegularFile(file)) throw missing();
            return new FileSystemResource(file);
        });
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options", "nosniff").body(resource);
    }

    public void persist(UUID id, byte[] bytes) {
        var data = new ProductMediaData();data.setMediaId(id);data.setContent(bytes);data.setCreatedAt(Instant.now());contents.saveAndFlush(data);
    }

    @Transactional
    public void remove(Principal principal, UUID id, UUID media) {
        if (repository.findProducts2(id, UUID.fromString(principal.getName())).isEmpty()) throw new MediaException(MarketplaceErrorCode.MEDIA_DRAFT_NOT_FOUND);
        if (repository.deleteProductMedia(media, id) != 1) throw missing();
        if (repository.markMediaEdited(id, UUID.fromString(principal.getName())) > 0) submissions.stateChanged(id);
        // The database FK removes bytes atomically; original local files remain available for backup.
    }

    private Map<String, Object> result(UUID id) { return Map.of("id", id, "url", "/api/public/product-media/" + id); }
    private MediaException missing() { return new MediaException(MarketplaceErrorCode.PHOTO_NOT_FOUND); }
}
