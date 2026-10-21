package vn.camerahub.identity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Private local development adapter. Keys are generated here and never accepted from clients. */
@Component
public class PrivateEvidenceStore {
    private final Path root;
    public PrivateEvidenceStore(@Value("${camerahub.storage.private-root:storage/private}") String directory) {
        root = Path.of(directory).toAbsolutePath().normalize();
    }

    public String put(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn một file JPG, PNG hoặc PDF, tối đa 5 MB.");
        }
        try {
            byte[] bytes = file.getBytes();
            String extension;
            if (bytes.length >= 5 && bytes[0]=='%' && bytes[1]=='P' && bytes[2]=='D' && bytes[3]=='F' && bytes[4]=='-') extension = ".pdf";
            else if (bytes.length >= 3 && (bytes[0]&255)==255 && (bytes[1]&255)==216 && (bytes[2]&255)==255) extension = ".jpg";
            else if (bytes.length >= 8 && java.util.Arrays.equals(java.util.Arrays.copyOf(bytes, 8),
                    new byte[]{(byte)137,80,78,71,13,10,26,10})) extension = ".png";
            else throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File phải có định dạng JPG, PNG hoặc PDF hợp lệ.");
            Files.createDirectories(root);
            String key = UUID.randomUUID() + extension;
            Files.write(resolve(key), bytes, java.nio.file.StandardOpenOption.CREATE_NEW);
            return key;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Chưa lưu được hồ sơ. Vui lòng thử lại.");
        }
    }

    public Path resolve(String key) {
        if (key == null || !key.matches("[a-f0-9-]{36}\\.(pdf|jpg|png)")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy file hồ sơ.");
        }
        Path file = root.resolve(key).normalize();
        if (!file.getParent().equals(root)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return file;
    }

    public void delete(String key) {
        try { Files.deleteIfExists(resolve(key)); }
        catch (IOException exception) { /* Unreferenced files can be reconciled without exposing private keys. */ }
    }
}
