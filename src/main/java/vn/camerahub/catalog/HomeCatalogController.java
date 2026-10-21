package vn.camerahub.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only home feed. Private serials, seller documents and storage keys are never selected. */
@RestController
@RequestMapping("/api/public")
public class HomeCatalogController {
    private final JdbcTemplate jdbc;

    public HomeCatalogController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/featured-products")
    public Feed featuredProducts() {
        List<Product> items = jdbc.query("""
                SELECT p.id, p.title, b.name AS brand, m.model_name, m.device_type,
                       p.price_vnd, p.shutter_count, p.condition_notes, p.inspection_badge
                FROM camerahub.products p
                JOIN camerahub.camera_models m ON m.id = p.model_id
                JOIN camerahub.brands b ON b.id = m.brand_id
                WHERE p.listing_status = 'PUBLISHED'
                ORDER BY p.created_at DESC, p.id
                LIMIT 24
                """, (row, index) -> new Product(row.getObject("id", UUID.class),
                row.getString("title"), row.getString("brand"), row.getString("model_name"),
                row.getString("device_type"), Long.toString(row.getLong("price_vnd")),
                row.getObject("shutter_count", Integer.class), row.getString("condition_notes"),
                row.getString("inspection_badge")));
        return new Feed(items);
    }

    public record Feed(List<Product> items) {}

    // Integer VND travels as a decimal string to avoid JavaScript number precision loss.
    public record Product(UUID id, String title, String brand, String modelName, String deviceType,
                          String priceVnd, Integer shutterCount, String conditionNotes,
                          String inspectionBadge) {}
}
