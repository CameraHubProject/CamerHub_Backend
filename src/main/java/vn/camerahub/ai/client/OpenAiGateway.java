package vn.camerahub.ai.client;

import vn.camerahub.ai.exception.*;

import vn.camerahub.ai.service.AiSettings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;


@Component
public class OpenAiGateway {
    private static final Logger log = LoggerFactory.getLogger(OpenAiGateway.class);
    private final AiSettings settings;
    private final ObjectMapper json;
    private final HttpClient http;
    private final URI endpoint;

    @Autowired
    public OpenAiGateway(AiSettings settings, ObjectMapper json) {
        this(settings, json, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build(), URI.create("https://api.openai.com/v1/responses"));
    }

    OpenAiGateway(AiSettings settings, ObjectMapper json, HttpClient http, URI endpoint) {
        this.settings = settings;
        this.json = json;
        this.http = http;
        this.endpoint = endpoint;
    }

    public JsonNode respond(Map<String, Object> body, Duration remaining) {
        try {
            var request = HttpRequest.newBuilder(endpoint).timeout(remaining).header("Authorization", "Bearer " + settings.key())
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String code = "unavailable", parameter = "unavailable";
                try {
                    var error = json.readTree(response.body()).path("error");
                    code = diagnostic(error.path("code").asText());
                    parameter = diagnostic(error.path("param").asText());
                } catch (Exception ignored) { /* Never log the raw response body or request content. */ }
                log.warn("OpenAI request rejected: status={}, code={}, parameter={}", response.statusCode(), code, parameter);
            }
            if (response.statusCode() == 429)
                throw new AdvisorException(AdvisorErrorCode.PROVIDER_QUOTA_EXHAUSTED);
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new AdvisorException(AdvisorErrorCode.PROVIDER_UNAVAILABLE);
            var result = json.readTree(response.body());
            if (result == null || !result.isObject())
                throw new AdvisorException(AdvisorErrorCode.INVALID_AI_RESPONSE);
            return result;
        } catch (vn.camerahub.shared.exception.AppException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AdvisorException(AdvisorErrorCode.REQUEST_INTERRUPTED);
        } catch (java.net.http.HttpTimeoutException e) {
            throw new AdvisorException(AdvisorErrorCode.PROVIDER_TIMEOUT);
        } catch (Exception e) {
            log.warn("OpenAI connection failed: {}", e.getClass().getSimpleName());
            throw new AdvisorException(AdvisorErrorCode.PROVIDER_UNAVAILABLE);
        }
    }

    private String diagnostic(String value) {
        return value.length() <= 80 && value.matches("[A-Za-z0-9_.\\[\\]-]*") ? value : "unavailable";
    }
}

