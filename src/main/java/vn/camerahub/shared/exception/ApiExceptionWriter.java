package vn.camerahub.shared.exception;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import vn.camerahub.shared.dto.ApiResponse;

@Component
@lombok.RequiredArgsConstructor
public class ApiExceptionWriter {
    private final ObjectMapper json;

    public void write(HttpServletResponse response, ErrorCode code) throws IOException {
        write(response, new AppException(code));
    }

    public void write(HttpServletResponse response, AppException error) throws IOException {
        response.setStatus(error.getStatusCode().value());
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        json.writeValue(response.getOutputStream(), ApiResponse.error(error.getErrorCode(), error.getErrors()));
    }

    public void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        json.writeValue(response.getOutputStream(), ApiResponse.error(status, message, Map.of()));
    }
}
