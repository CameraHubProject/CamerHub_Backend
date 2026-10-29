package vn.camerahub.identity.service;

import java.io.*;
import java.time.Clock;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.camerahub.identity.repository.AccountRepository;
import vn.camerahub.shared.exception.*;

@Service
@lombok.RequiredArgsConstructor
public class AvatarService {
    private final AccountRepository repository;
    private final Clock clock;

    @Transactional(rollbackFor = IOException.class)
    public void upload(UUID user, MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 2 * 1024 * 1024) throw invalid("Ảnh phải có dung lượng từ 1 byte đến 2 MB.");
        String name = Objects.toString(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        String type = file.getContentType();
        if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp"))) throw invalid("Chỉ chấp nhận JPG, PNG hoặc WebP.");
        byte[] content;
        try (var input = file.getInputStream()) { content = input.readNBytes(2 * 1024 * 1024 + 1); }
        if (content.length > 2 * 1024 * 1024) throw invalid("Ảnh tối đa 2 MB.");
        String detected = detect(content);
        if (!detected.equals(type) || !(detected.equals("image/jpeg") ? name.endsWith(".jpg") || name.endsWith(".jpeg") : name.endsWith(detected.equals("image/png") ? ".png" : ".webp")))
            throw invalid("Định dạng ảnh không khớp nội dung.");
        var account = repository.lockById(user).orElseThrow(() -> new AppException(CommonErrorCode.RESOURCE_NOT_FOUND));
        // Only generated UUID keys are stored; the supplied filename never becomes a path.
        // Replacing all three fields deletes the previous image in the same DB transaction.
        account.setAvatarKey(UUID.randomUUID());
        account.setAvatarContent(content);
        account.setAvatarType(detected);
        account.setUpdatedAt(clock.instant());
        repository.saveAndFlush(account);
    }

    private String detect(byte[] bytes) throws IOException {
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw invalid("Nội dung ảnh không hợp lệ.");
            var reader = readers.next();
            try {
                String format = reader.getFormatName().toUpperCase(Locale.ROOT);
                if (!Set.of("JPEG", "PNG", "WEBP").contains(format)) throw invalid("Chỉ chấp nhận JPG, PNG hoặc WebP.");
                reader.setInput(stream, true, true);
                dimensions(reader.getWidth(0), reader.getHeight(0));
                if (reader.read(0) == null) throw invalid("Nội dung ảnh không hợp lệ.");
                return switch (format) { case "JPEG" -> "image/jpeg"; case "PNG" -> "image/png"; default -> "image/webp"; };
            } finally { reader.dispose(); }
        } catch (IOException | IndexOutOfBoundsException error) { throw invalid("Không đọc được ảnh."); }
    }

    private void dimensions(int width, int height) {
        if (width < 1 || height < 1 || (long)width * height > 24_000_000) throw invalid("Kích thước ảnh không hợp lệ.");
    }
    private ValidationException invalid(String message) { return new ValidationException(Map.of("file", message)); }
}
