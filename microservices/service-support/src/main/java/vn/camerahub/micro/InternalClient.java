package vn.camerahub.micro;

import com.fasterxml.jackson.databind.*;

import java.net.http.*;
import java.time.Duration;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import vn.camerahub.micro.exception.*;

@Component
public class InternalClient {
    private final ServiceDirectory directory;
    private final ObjectMapper json;
    private final String key;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();

    public InternalClient(ServiceDirectory directory, ObjectMapper json, @Value("${camerahub.internal.key}") String key) {
        if (key.length() < 32)
            throw new IllegalArgumentException("SERVICE_INTERNAL_KEY must contain at least 32 characters");
        this.directory = directory;
        this.json = json;
        this.key = key;
    }

    public JsonNode get(String service, String path) {
        return call(service, path, "GET", null, Map.of());
    }

    public JsonNode post(String service, String path, Object body) {
        return call(service, path, "POST", body, Map.of());
    }

    public JsonNode call(String service, String path, String method, Object body, Map<String, String> headers) {
        try {
            var value = json.readTree(send(service, path, method, body == null ? null : json.writeValueAsBytes(body), headers).body());
            return value != null && value.has("code") && value.has("data") ? value.path("data") : value;
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw unavailable();
        }
    }

    public byte[] bytes(String service, String path) {
        return send(service, path, "GET", null, Map.of()).body();
    }

    private HttpResponse<byte[]> send(String service, String path, String method, byte[] body, Map<String, String> headers) {
        try {
            var request = HttpRequest.newBuilder(directory.uri(service, path)).timeout(Duration.ofSeconds(15))
                    .header("X-CameraHub-Key", key).header("Content-Type", "application/json");
            headers.forEach(request::header);
            request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body));
            var result = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (result.statusCode() < 200 || result.statusCode() >= 300) {
                var status = HttpStatus.resolve(result.statusCode());
                if (status == null) status = HttpStatus.BAD_GATEWAY;
                var fallback = CommonErrorCode.forStatus(status.value());
                String message = fallback.getMessage(), code = fallback.getCode();
                Map<String, String> fields = new LinkedHashMap<>();
                try {
                    var error = json.readTree(result.body());
                    message = error.path("message").asText(message);
                    code = error.path("errorCode").asText(code);
                    if (error.path("errors").isObject()) error.path("errors").fields().forEachRemaining(field -> {
                        if (field.getValue().isTextual()) fields.put(field.getKey(), field.getValue().asText());
                    });
                } catch (Exception ignored) {
                }
                throw new RemoteServiceException(status, code, message, fields);
            }
            return result;
        } catch (AppException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (Exception e) {
            throw unavailable();
        }
    }

    private AppException unavailable() {
        return new AppException(CommonErrorCode.SERVICE_UNAVAILABLE);
    }
}

