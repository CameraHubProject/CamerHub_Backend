package vn.camerahub.gateway.service;

import jakarta.servlet.http.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@org.springframework.stereotype.Service
public class GatewayService {
    private final Map<String, URI> targets;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static final Set<String> HOP = Set.of("connection", "content-length", "host", "keep-alive", "proxy-authenticate", "proxy-authorization", "te", "trailer", "transfer-encoding", "upgrade");

    public GatewayService(@Value("${camerahub.services.identity}") URI identity,
                          @Value("${camerahub.services.marketplace}") URI marketplace,
                          @Value("${camerahub.services.moderation}") URI moderation,
                          @Value("${camerahub.services.commerce}") URI commerce,
                          @Value("${camerahub.services.advisor}") URI advisor) {
        targets = Map.of("identity", identity, "marketplace", marketplace, "moderation", moderation, "commerce", commerce, "advisor", advisor);
    }

    public static String route(String path) {
        if (path == null || path.contains("%") || path.contains(";") || path.contains("..") || path.contains("//") || path.contains("\\"))
            return null;
        if (path.startsWith("/api/auth/") || path.equals("/api/marketplace/addresses")) return "identity";
        if (path.equals("/api/marketplace/checkout") || path.startsWith("/api/marketplace/orders") || path.equals("/api/marketplace/buyback") || path.equals("/api/public/showrooms") || path.matches("/api/public/sellers/[a-fA-F0-9-]+/reviews"))
            return "commerce";
        if (path.equals("/api/admin/listings") || path.matches("/api/admin/listings/[a-fA-F0-9-]+/review"))
            return "moderation";
        if (path.startsWith("/api/ai/")) return "advisor";
        if (path.startsWith("/api/public/products") || path.startsWith("/api/public/product-media/") || path.startsWith("/api/public/sellers/") || Set.of("/api/public/featured-products", "/api/public/models", "/api/public/brands").contains(path) || path.startsWith("/api/marketplace/listings") || path.startsWith("/api/admin/brands"))
            return "marketplace";
        return null;
    }

    public static boolean allowedHeader(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return !HOP.contains(lower) && !lower.startsWith("x-camerahub-") && !lower.startsWith("x-forwarded-") && !Set.of("forwarded", "proxy-connection").contains(lower);
    }

    public Object health() {
        return Map.of("status", "UP", "service", "api-gateway");
    }

    public void forward(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getRequestURI(), target = route(path);
        if (target == null) {
            error(response, 404, "Không tìm thấy API yêu cầu.");
            return;
        }
        if (!Set.of("GET", "HEAD", "POST", "DELETE", "PUT", "PATCH", "OPTIONS").contains(request.getMethod())) {
            error(response, 405, "Phương thức HTTP không được hỗ trợ.");
            return;
        }
        byte[] body = request.getInputStream().readNBytes(6 * 1024 * 1024 + 1);
        if (body.length > 6 * 1024 * 1024) {
            error(response, 413, "Dữ liệu gửi lên vượt quá giới hạn cho phép.");
            return;
        }
        try {
            var uri = URI.create(targets.get(target).toString() + path + (request.getQueryString() == null ? "" : "?" + request.getQueryString()));
            var outgoing = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(target.equals("advisor") ? 90 : 30)).method(request.getMethod(), body.length == 0 ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body));
            var excluded = new HashSet<String>();
            var connection = request.getHeader("Connection");
            if (connection != null)
                for (String name : connection.split(",")) excluded.add(name.strip().toLowerCase(Locale.ROOT));
            for (String name : Collections.list(request.getHeaderNames()))
                if (allowedHeader(name) && !excluded.contains(name.toLowerCase(Locale.ROOT)))
                    for (String value : Collections.list(request.getHeaders(name))) outgoing.header(name, value);
            var result = client.send(outgoing.build(), HttpResponse.BodyHandlers.ofInputStream());
            response.setStatus(result.statusCode());
            Set<String> excludedResponse = new HashSet<>();
            result.headers().allValues("connection").forEach(value -> {
                for (String name : value.split(",")) excludedResponse.add(name.strip().toLowerCase(Locale.ROOT));
            });
            result.headers().map().forEach((name, values) -> {
                if (allowedHeader(name) && !excludedResponse.contains(name.toLowerCase(Locale.ROOT)))
                    values.forEach(value -> response.addHeader(name, value));
            });
            try (var stream = result.body()) {
                stream.transferTo(response.getOutputStream());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            error(response, 503, "Dịch vụ tạm thời gián đoạn. Vui lòng thử lại.");
        } catch (Exception e) {
            if (!response.isCommitted())
                error(response, 503, "Dịch vụ đang khởi động hoặc tạm thời gián đoạn. Vui lòng thử lại.");
        }
    }

    private void error(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        String errorCode = switch (code) {
            case 404 -> "RESOURCE_NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 413 -> "UPLOAD_TOO_LARGE";
            default -> "SERVICE_UNAVAILABLE";
        };
        response.getWriter().write("{\"code\":%d,\"errorCode\":\"%s\",\"message\":\"%s\",\"data\":null,\"errors\":{},\"timestamp\":\"%s\"}".formatted(code, errorCode, message, java.time.Instant.now()));
    }
}

