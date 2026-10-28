package vn.camerahub.marketplace.mapper;

import java.util.*;

import vn.camerahub.shared.persistence.JpaRow;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@org.springframework.stereotype.Component
@lombok.RequiredArgsConstructor
public class CatalogMapper {
    private final ObjectMapper json;

    public Map<String, Object> product(JpaRow row, int index) {
        var item = new LinkedHashMap<String, Object>();
        item.put("id", row.getString("id"));
        item.put("title", row.getString("title"));
        item.put("sellerId", row.getString("seller_id"));
        item.put("sellerName", row.getString("seller_name"));
        item.put("brand", row.getString("brand"));
        item.put("modelName", row.getString("model_name"));
        item.put("deviceType", row.getString("device_type"));
        item.put("priceVnd", row.getString("price_vnd"));
        item.put("shutterCount", row.getObject("shutter_count"));
        item.put("conditionNotes", row.getString("condition_notes"));
        item.put("inspectionBadge", row.getString("inspection_badge"));
        item.put("description", row.getString("description"));
        item.put("accessories", row.getString("accessories"));
        item.put("repairHistory", row.getString("repair_history"));
        item.put("listingStatus", row.getString("listing_status"));
        item.put("presentation", parse(row.getString("presentation")));
        item.put("specs", parse(row.getString("specs")));
        item.put("inspectionChecks", parseList(row.getString("inspection_checks")));
        item.put("images", java.util.List.of());
        return item;
    }

    public Map<String, Object> moderatedProduct(JpaRow row, int index) {
        var item = product(row, index);
        item.put("moderationSource", row.getString("moderation_source"));
        item.put("moderationNote", row.getString("moderation_note"));
        return item;
    }

    private Object parseList(String value) {
        try {
            return json.readValue(value, Object.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Object> parse(String value) {
        try {
            return json.readValue(value, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("Stored JSON is invalid", e);
        }
    }

}
