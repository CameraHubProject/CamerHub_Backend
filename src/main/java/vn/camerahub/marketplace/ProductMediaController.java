package vn.camerahub.marketplace;

import javax.imageio.ImageIO;
import java.nio.file.*;
import java.security.Principal;
import java.util.*;
import java.io.IOException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ProductMediaController {
    private final JdbcTemplate jdbc;
    private final MarketplaceService marketplace;
    private final Path root=Path.of("storage","product-media").toAbsolutePath().normalize();
    public ProductMediaController(JdbcTemplate jdbc,MarketplaceService marketplace) { this.jdbc=jdbc; this.marketplace=marketplace; }

    @PostMapping("/api/marketplace/listings/{id}/media")
    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> upload(Principal principal,@PathVariable UUID id,@RequestParam("file") MultipartFile file) throws IOException {
        UUID user=UUID.fromString(principal.getName()); marketplace.requireSeller(user);
        var products=jdbc.queryForList("SELECT id FROM camerahub.products WHERE id=? AND seller_id=? AND listing_status IN ('DRAFT','PENDING_REVIEW','NEEDS_REVISION') FOR UPDATE",id,user);
        if(products.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Không tìm thấy tin đăng được phép bổ sung ảnh.");
        if(file.isEmpty() || file.getSize()>5*1024*1024) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Ảnh tối đa 5 MB.");
        if(jdbc.queryForObject("SELECT count(*) FROM camerahub.product_media WHERE product_id=?",Integer.class,id)>=8)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Mỗi tin đăng tối đa 8 ảnh.");
        // Inspect encoded dimensions before decoding, and re-encode to strip metadata and active payloads.
        java.awt.image.BufferedImage decoded;
        try(var stream=ImageIO.createImageInputStream(file.getInputStream())) {
            if(stream==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Ảnh không hợp lệ.");
            var readers=ImageIO.getImageReaders(stream);
            if(!readers.hasNext()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chỉ nhận ảnh JPG hoặc PNG.");
            var reader=readers.next();
            try {
                if(!Set.of("JPEG","PNG").contains(reader.getFormatName().toUpperCase(Locale.ROOT)))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chỉ nhận ảnh JPG hoặc PNG.");
                reader.setInput(stream,true,true);
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<1 || height<1 || (long)width*height>24_000_000)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Ảnh tối đa 24 megapixel.");
                decoded=reader.read(0);
            } finally { reader.dispose(); }
        }
        UUID media=UUID.randomUUID(); Files.createDirectories(root);
        String name=media+".png"; Path destination=root.resolve(name);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if(status!=STATUS_COMMITTED) try { Files.deleteIfExists(destination); } catch(IOException ignored) { /* Retain an unreferenced file rather than breaking rollback. */ }
            }
        });
        try(var output=Files.newOutputStream(destination,StandardOpenOption.CREATE_NEW)) {
            if(!ImageIO.write(decoded,"png",output)) throw new IOException("PNG encoder unavailable");
        }
        jdbc.update("INSERT INTO camerahub.product_media(id,product_id,storage_key,media_type,sort_order) VALUES (?,?,?,'IMAGE',(SELECT count(*) FROM camerahub.product_media WHERE product_id=?))",media,id,"product-public/"+name,id);
        return Map.of("id",media,"url","/api/public/product-media/"+media);
    }

    @GetMapping("/api/public/product-media/{id}")
    public ResponseEntity<FileSystemResource> image(Authentication authentication,@PathVariable UUID id) {
        String owner=authentication==null?"":authentication.getName();
        boolean admin=authentication!=null && authentication.getAuthorities().stream().anyMatch(role->role.getAuthority().equals("ROLE_ADMIN"));
        var rows=jdbc.queryForList("""
            SELECT pm.storage_key FROM camerahub.product_media pm JOIN camerahub.products p ON p.id=pm.product_id
            WHERE pm.id=? AND (p.listing_status='PUBLISHED' OR p.seller_id::text=? OR ?)
            """,id,owner,admin);
        String expected="product-public/"+id+".png";
        if(rows.isEmpty() || !expected.equals(rows.getFirst().get("storage_key"))) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        Path file=root.resolve(id+".png");
        if(!Files.isRegularFile(file)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG)
            .header("X-Content-Type-Options","nosniff").body(new FileSystemResource(file));
    }
}
