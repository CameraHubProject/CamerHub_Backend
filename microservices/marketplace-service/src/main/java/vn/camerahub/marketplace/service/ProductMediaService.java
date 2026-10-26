package vn.camerahub.marketplace.service;

import vn.camerahub.marketplace.exception.*;

import vn.camerahub.marketplace.util.ImageFingerprint;
import vn.camerahub.marketplace.repository.ProductMediaRepository;

import javax.imageio.ImageIO;
import java.nio.file.*;
import java.security.Principal;
import java.util.*;
import java.io.IOException;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@org.springframework.stereotype.Service
public class ProductMediaService {
    private final ProductMediaRepository repository;
    private final Path root;

    public ProductMediaService(ProductMediaRepository repository, @org.springframework.beans.factory.annotation.Value("${camerahub.media-root:storage/product-media}") String root) {
        this.repository = repository;
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> upload(Principal principal, UUID id, MultipartFile file) throws IOException {
        UUID user = UUID.fromString(principal.getName());
        var products = repository.findProducts(id, user);
        if (products.isEmpty())
            throw new MediaException(MarketplaceErrorCode.PHOTO_UPLOAD_NOT_ALLOWED);
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
            throw new MediaException(MarketplaceErrorCode.PHOTO_TOO_LARGE);
        // Inspect encoded dimensions before decoding, and re-encode to strip metadata and active payloads.
        java.awt.image.BufferedImage decoded;
        try (var stream = ImageIO.createImageInputStream(file.getInputStream())) {
            if (stream == null) throw new MediaException(MarketplaceErrorCode.INVALID_PHOTO);
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext())
                throw new MediaException(MarketplaceErrorCode.UNSUPPORTED_PHOTO_FORMAT);
            var reader = readers.next();
            try {
                if (!Set.of("JPEG", "PNG").contains(reader.getFormatName().toUpperCase(Locale.ROOT)))
                    throw new MediaException(MarketplaceErrorCode.UNSUPPORTED_PHOTO_FORMAT);
                reader.setInput(stream, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > 24_000_000)
                    throw new MediaException(MarketplaceErrorCode.PHOTO_DIMENSIONS_EXCEEDED);
                decoded = reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (javax.imageio.IIOException exception) {
            throw new MediaException(MarketplaceErrorCode.UNREADABLE_PHOTO);
        }
        var fingerprint = ImageFingerprint.of(decoded);
        var existing = repository.findProductMedia(UUID.class, id, fingerprint.sha256());
        if (!existing.isEmpty())
            return Map.of("id", existing.getFirst(), "url", "/api/public/product-media/" + existing.getFirst());
        if (repository.findProductMedia2(Integer.class, id) >= 8)
            throw new MediaException(MarketplaceErrorCode.PHOTO_LIMIT_EXCEEDED);
        UUID media = UUID.randomUUID();
        Files.createDirectories(root);
        String name = media + ".png";
        Path destination = root.resolve(name);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) try {
                    Files.deleteIfExists(destination);
                } catch (IOException ignored) { /* Retain an unreferenced file rather than breaking rollback. */ }
            }
        });
        try (var output = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
            if (!ImageIO.write(decoded, "png", output)) throw new IOException("PNG encoder unavailable");
        }
        repository.insertProductMedia(media, id, "product-public/" + name, id, fingerprint.sha256(), fingerprint.dhash(), fingerprint.signature());
        return Map.of("id", media, "url", "/api/public/product-media/" + media);
    }

    public ResponseEntity<FileSystemResource> image(Authentication authentication, UUID id) {
        String owner = authentication == null ? "" : authentication.getName();
        boolean admin = authentication != null && authentication.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals("ROLE_ADMIN"));
        var rows = repository.findProductMedia3(id, owner, admin);
        String expected = "product-public/" + id + ".png";
        if (rows.isEmpty() || !expected.equals(rows.getFirst().get("storage_key")))
            throw new MediaException(MarketplaceErrorCode.PHOTO_NOT_FOUND);
        Path file = root.resolve(id + ".png");
        if (!Files.isRegularFile(file)) throw new MediaException(MarketplaceErrorCode.PHOTO_NOT_FOUND);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
                .header("X-Content-Type-Options", "nosniff").body(new FileSystemResource(file));
    }

    public ResponseEntity<FileSystemResource> internalImage(UUID id) {
        var rows = repository.findProductMedia4(String.class, id);
        if (rows.isEmpty() || !("product-public/" + id + ".png").equals(rows.getFirst()) || !Files.isRegularFile(root.resolve(id + ".png")))
            throw new MediaException(MarketplaceErrorCode.PHOTO_NOT_FOUND);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore()).body(new FileSystemResource(root.resolve(id + ".png")));
    }

    @Transactional
    public void remove(Principal principal, UUID id, UUID media) {
        var rows = repository.findProducts2(id, UUID.fromString(principal.getName()));
        if (rows.isEmpty())
            throw new MediaException(MarketplaceErrorCode.MEDIA_DRAFT_NOT_FOUND);
        if (repository.deleteProductMedia(media, id) != 1)
            throw new MediaException(MarketplaceErrorCode.PHOTO_NOT_FOUND);
        Path destination = root.resolve(media + ".png");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    Files.deleteIfExists(destination);
                } catch (IOException ignored) { /* Unreferenced files remain inaccessible. */ }
            }
        });
    }
}

