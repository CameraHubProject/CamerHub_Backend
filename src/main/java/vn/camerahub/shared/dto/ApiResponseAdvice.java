package vn.camerahub.shared.dto;

import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.server.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "vn.camerahub")
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) {
        return !method.getContainingClass().getPackageName().contains(".exception");
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter method, MediaType media, Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request, ServerHttpResponse response) {
        String path = request.getURI().getPath();
        if (!path.startsWith("/api/") || path.equals("/api/health") || path.startsWith("/api/public/product-media/") || body == null || body instanceof ApiResponse || body instanceof org.springframework.core.io.Resource || body instanceof byte[] || body instanceof String)
            return body;
        int code = response instanceof ServletServerHttpResponse servlet ? servlet.getServletResponse().getStatus() : 200;
        var notification = method.getMethodAnnotation(SuccessMessage.class);
        return ApiResponse.success(code, notification == null ? "Thành công." : notification.value(), body);
    }
}
