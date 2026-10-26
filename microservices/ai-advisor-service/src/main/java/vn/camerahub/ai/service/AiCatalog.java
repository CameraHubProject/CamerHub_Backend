package vn.camerahub.ai.service;

import com.fasterxml.jackson.databind.*;

import java.util.*;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import vn.camerahub.micro.InternalClient;

/**
 * Public catalog contract; this service has no credentials for Marketplace's database.
 */
@Component
@lombok.RequiredArgsConstructor
public class AiCatalog {
    private final InternalClient client;
    private final ObjectMapper json;

    public List<Map<String, Object>> search(String query, Long min, Long max, String type) {
        var result = new ArrayList<Map<String, Object>>();
        for (var item : client.get("marketplace", "/api/public/products").path("items")) {
            long price = item.path("priceVnd").asLong();
            String text = (item.path("title").asText() + " " + item.path("brand").asText() + " " + item.path("modelName").asText() + " " + item.path("description").asText()).toLowerCase(Locale.ROOT);
            if ((min == null || price >= min) && (max == null || price <= max) && (type == null || type.isBlank() || type.equals(item.path("deviceType").asText())) && (query == null || text.contains(query.strip().toLowerCase(Locale.ROOT))))
                result.add(publicItem(item));
            if (result.size() == 8) break;
        }
        return result;
    }

    public Map<String, Object> detail(UUID id) {
        try {
            return publicItem(client.get("marketplace", "/api/public/products/" + id));
        } catch (vn.camerahub.micro.exception.AppException e) {
            if (e.getStatusCode().value() == 404) return Map.of("id", id, "available", false);
            throw e;
        }
    }

    public long count() {
        return client.get("marketplace", "/api/public/products").path("items").size();
    }

    public List<Map<String, Object>> call(String name, JsonNode arguments) {
        if ("get_camerahub_product".equals(name)) {
            try {
                return List.of(detail(UUID.fromString(arguments.path("id").asText())));
            } catch (IllegalArgumentException e) {
                return List.of(Map.of("error", "Mã sản phẩm không hợp lệ."));
            }
        }
        if (!"search_camerahub_products".equals(name)) return List.of(Map.of("error", "Công cụ không được hỗ trợ."));
        String query = arguments.path("query").asText("");
        Long min = amount(arguments.get("min_price_vnd")), max = amount(arguments.get("max_price_vnd"));
        String type = arguments.path("device_type").asText(null);
        if (query.length() > 160 || (type != null && !Set.of("BODY", "LENS", "OTHER").contains(type)) || (min != null && max != null && min > max))
            return List.of(Map.of("error", "Bộ lọc tìm kiếm không hợp lệ."));
        return search(query, min, max, type);
    }

    private Long amount(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (!n.isIntegralNumber() || !n.canConvertToLong() || n.asLong() < 0 || n.asLong() > 9999999999999L)
            throw new IllegalArgumentException("Invalid price");
        return n.asLong();
    }

    private Map<String, Object> publicItem(JsonNode node) {
        var item = new LinkedHashMap<String, Object>();
        for (String key : List.of("id", "title", "brand", "modelName", "deviceType", "priceVnd", "shutterCount", "inspectionBadge", "listingStatus", "specs", "presentation", "images"))
            item.put(key, json.convertValue(node.path(key), Object.class));
        for (String key : List.of("description", "conditionNotes", "accessories")) {
            String value = node.path(key).asText("");
            item.put(key, value.substring(0, Math.min(value.length(), key.equals("description") ? 1200 : 700)));
        }
        item.put("available", "PUBLISHED".equals(node.path("listingStatus").asText()));
        item.put("url", "/may-anh/" + node.path("id").asText());
        return item;
    }
}

