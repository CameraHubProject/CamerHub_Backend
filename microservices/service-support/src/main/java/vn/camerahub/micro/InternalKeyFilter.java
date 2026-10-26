package vn.camerahub.micro;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.web.filter.OncePerRequestFilter;

public class InternalKeyFilter extends OncePerRequestFilter {
    private final byte[] key;

    public InternalKeyFilter(String key) {
        this.key = key.getBytes(StandardCharsets.UTF_8);
    }

    public static boolean matches(String supplied, byte[] expected) {
        return supplied != null && MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8), expected);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/internal/") && !matches(request.getHeader("X-CameraHub-Key"), key)) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"errorCode\":\"INTERNAL_ACCESS_DENIED\",\"message\":\"Không được truy cập API nội bộ.\",\"data\":null,\"errors\":{}}");
            return;
        }
        chain.doFilter(request, response);
    }
}

