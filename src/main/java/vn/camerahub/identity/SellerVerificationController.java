package vn.camerahub.identity;

import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SellerVerificationController {
    private final SellerVerificationService verifications;
    public SellerVerificationController(SellerVerificationService verifications) { this.verifications = verifications; }

    @GetMapping("/api/seller-verification")
    public List<SellerVerificationService.Verification> mine(Authentication auth) { return verifications.mine(id(auth)); }

    @PostMapping(value="/api/seller-verification", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SellerVerificationService.Verification submit(Authentication auth, @RequestParam("file") MultipartFile file) {
        return verifications.submit(id(auth), file);
    }

    @GetMapping("/api/admin/seller-verifications")
    public List<SellerVerificationService.Verification> queue() { return verifications.queue(); }

    @PostMapping("/api/admin/seller-verifications/{verificationId}/review")
    public SellerVerificationService.Verification review(Authentication auth, @PathVariable UUID verificationId, @RequestBody Review review) {
        return verifications.review(id(auth), verificationId, review.decision(), review.note());
    }

    @GetMapping("/api/seller-verification/{verificationId}/evidence")
    public ResponseEntity<FileSystemResource> evidence(Authentication auth, @PathVariable UUID verificationId) {
        var path = verifications.evidence(id(auth), auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")), verificationId);
        if (!Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy file hồ sơ.");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ho-so" + path.toString().substring(path.toString().lastIndexOf('.')) + "\"")
                .body(new FileSystemResource(path));
    }

    private static UUID id(Authentication auth) { return UUID.fromString(auth.getName()); }
    public record Review(String decision, String note) {}
}
