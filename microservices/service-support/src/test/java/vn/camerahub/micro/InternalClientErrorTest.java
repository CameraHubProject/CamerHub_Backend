package vn.camerahub.micro;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import vn.camerahub.micro.exception.RemoteServiceException;

import static org.assertj.core.api.Assertions.*;

class InternalClientErrorTest {
    @Test
    void downstreamErrorsKeepTheirDomainCodeMessageAndFields() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/test", exchange -> {
            byte[] body = """
                    {"code":409,"errorCode":"IDENTITY_PHONE_ALREADY_USED","message":"Số điện thoại đã được sử dụng.","data":null,"errors":{"phone":"Số điện thoại đã được sử dụng."}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(409, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://127.0.0.1:" + server.getAddress().getPort();
            var client = new InternalClient(new ServiceDirectory(url, url, url, url, url), new ObjectMapper(), "x".repeat(32));
            var error = catchThrowableOfType(() -> client.get("identity", "/test"), RemoteServiceException.class);
            assertThat(error.getStatusCode().value()).isEqualTo(409);
            assertThat(error.getErrorCode().getCode()).isEqualTo("IDENTITY_PHONE_ALREADY_USED");
            assertThat(error.getMessage()).isEqualTo("Số điện thoại đã được sử dụng.");
            assertThat(error.getErrors()).containsEntry("phone", "Số điện thoại đã được sử dụng.");
        } finally {
            server.stop(0);
        }
    }
}
