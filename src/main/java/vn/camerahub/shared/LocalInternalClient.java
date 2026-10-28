package vn.camerahub.shared;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import vn.camerahub.identity.service.AccountService;
import vn.camerahub.marketplace.dto.ReservationDtos.Reserve;
import vn.camerahub.marketplace.service.*;
import vn.camerahub.commerce.service.ReviewService;
import vn.camerahub.moderation.service.ModerationService;
import vn.camerahub.shared.exception.*;

@Component
@lombok.RequiredArgsConstructor
public class LocalInternalClient extends InternalClient {
    private final ObjectMapper json;
    private final ObjectProvider<AccountService> accounts;
    private final ObjectProvider<MarketplaceService> marketplace;
    private final ObjectProvider<MarketplaceEventService> marketplaceEvents;
    private final ObjectProvider<ReservationService> reservations;
    private final ObjectProvider<ProductMediaService> media;
    private final ObjectProvider<ReviewService> commerce;
    private final ObjectProvider<ModerationService> moderation;

    @Override
    public JsonNode call(String module, String path, String method, Object body, Map<String, String> headers) {
        URI uri = URI.create(path);
        String route = uri.getPath();
        Object value = null;
        if ("identity".equals(module) && "GET".equals(method) && route.startsWith("/internal/accounts/")) {
            value = accounts.getObject().publicData(id(route));
        } else if ("marketplace".equals(module)) {
            if ("GET".equals(method) && route.equals("/api/public/products")) value = Map.of("items", marketplace.getObject().products());
            else if ("GET".equals(method) && route.startsWith("/api/public/products/")) value = marketplace.getObject().detail(id(route));
            else if ("GET".equals(method) && route.equals("/internal/index-snapshots")) {
                String query = uri.getRawQuery();
                value = marketplaceEvents.getObject().snapshots(query == null ? 0 : Integer.parseInt(query.replaceFirst("^offset=", "")));
            } else if ("POST".equals(method) && route.equals("/internal/events")) value = marketplaceEvents.getObject().receive(json.convertValue(body, IntegrationEvents.Event.class));
            else if ("POST".equals(method) && route.equals("/internal/reservations")) value = reservations.getObject().reserve(json.convertValue(body, Reserve.class));
            else if ("POST".equals(method) && route.matches("/internal/reservations/[^/]+/(confirm|release)")) {
                UUID batch = UUID.fromString(route.split("/")[3]);
                value = route.endsWith("/confirm") ? reservations.getObject().confirm(batch) : reservations.getObject().release(batch);
            } else if ("POST".equals(method) && route.matches("/internal/reservations/[^/]+/[^/]+/cancel")) {
                String[] parts = route.split("/");
                value = reservations.getObject().cancel(UUID.fromString(parts[3]), UUID.fromString(parts[4]), json.convertValue(body, new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {}));
            }
        } else if ("commerce".equals(module) && "GET".equals(method)) {
            if (route.startsWith("/internal/checkouts/")) value = commerce.getObject().checkout(id(route));
            else if (route.startsWith("/internal/orders/")) value = commerce.getObject().order(id(route));
            else if (route.matches("/internal/sellers/[^/]+/stats")) value = commerce.getObject().stats(UUID.fromString(route.split("/")[3]));
        } else if ("moderation".equals(module) && "POST".equals(method) && route.equals("/internal/events")) {
            moderation.getObject().receive(json.convertValue(body, IntegrationEvents.Event.class));
            value = Map.of("accepted", true);
        }
        if (value == null) throw new IllegalArgumentException("Unsupported module operation: " + module + " " + method + " " + route);
        return json.valueToTree(value);
    }

    @Override
    public byte[] bytes(String module, String path) {
        if (!"marketplace".equals(module) || !path.startsWith("/internal/media/")) throw new IllegalArgumentException("Unsupported media operation");
        var resource = media.getObject().internalImage(id(path)).getBody();
        if (resource == null) throw new AppException(CommonErrorCode.RESOURCE_NOT_FOUND);
        try (var input = resource.getInputStream()) { return input.readAllBytes(); }
        catch (IOException error) { throw new AppException(CommonErrorCode.SERVICE_UNAVAILABLE); }
    }

    private UUID id(String path) { return UUID.fromString(path.substring(path.lastIndexOf('/') + 1)); }
}
