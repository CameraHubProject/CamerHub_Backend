package vn.camerahub.gateway.controller;

import vn.camerahub.gateway.service.GatewayService;

import java.net.*;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

import org.springframework.mock.web.*;

class GatewayControllerTest {
    @Test
    void routesRespectServiceOwnershipAndRejectInternalPaths() {
        assertThat(GatewayService.route("/api/auth/me")).isEqualTo("identity");
        assertThat(GatewayService.route("/api/marketplace/addresses")).isEqualTo("identity");
        assertThat(GatewayService.route("/api/marketplace/orders/a/seller-review")).isEqualTo("commerce");
        assertThat(GatewayService.route("/api/public/sellers/abc-123/reviews")).isEqualTo("commerce");
        assertThat(GatewayService.route("/api/public/sellers/abc-123/listings")).isEqualTo("marketplace");
        assertThat(GatewayService.route("/api/admin/listings/abc-123/review")).isEqualTo("moderation");
        assertThat(GatewayService.route("/internal/events")).isNull();
        assertThat(GatewayService.route("/api/auth/../internal/session")).isNull();
        assertThat(GatewayService.route("/api/auth/%2e%2e/internal/session")).isNull();
        assertThat(GatewayService.route("/api/seller-verification")).isNull();
    }

    @Test
    void internalAndForwardedHeadersCannotBeSpoofed() {
        for (String name : new String[]{"X-CameraHub-Key", "x-camerahub-user", "Forwarded", "x-forwarded-for", "Host", "Connection"})
            assertThat(GatewayService.allowedHeader(name)).isFalse();
        assertThat(GatewayService.allowedHeader("X-CSRF-TOKEN")).isTrue();
        assertThat(GatewayService.allowedHeader("Cookie")).isTrue();
    }

    @Test
    void proxyPreservesCsrfCookiesAndMultipartBytesWithoutTrustingInternalKey() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/auth/login", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("X-CameraHub-Key")).isNull();
            assertThat(exchange.getRequestHeaders().getFirst("X-CSRF-TOKEN")).isEqualTo("csrf");
            byte[] data = exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().add("Set-Cookie", "one=1; HttpOnly");
            exchange.getResponseHeaders().add("Set-Cookie", "two=2; HttpOnly");
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        });
        server.start();
        try {
            URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            var gateway = new GatewayService(url, url, url, url, url);
            var request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.addHeader("X-CameraHub-Key", "attacker");
            request.addHeader("X-CSRF-TOKEN", "csrf");
            request.setContent("raw multipart bytes".getBytes());
            var response = new MockHttpServletResponse();
            gateway.forward(request, response);
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentAsString()).isEqualTo("raw multipart bytes");
            assertThat(response.getHeaders("Set-Cookie")).hasSize(2);
        } finally {
            server.stop(0);
        }
    }
}
