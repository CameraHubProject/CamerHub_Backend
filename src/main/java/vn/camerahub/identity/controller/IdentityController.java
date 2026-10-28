package vn.camerahub.identity.controller;

import java.security.Principal;
import java.util.UUID;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import vn.camerahub.identity.dto.IdentityDtos.*;
import vn.camerahub.identity.dto.response.ProfileResponse;
import vn.camerahub.identity.mapper.ProfileMapper;
import vn.camerahub.identity.service.*;
import vn.camerahub.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Accounts", description = "Đăng ký, JWT và thông tin cá nhân")
@lombok.RequiredArgsConstructor
public class IdentityController {
    private final AccountService accounts;
    private final ProfileMapper mapper;
    private final AuthTokenService tokens;

    @GetMapping("/csrf")
    @Operation(summary = "Lấy CSRF token cho trình duyệt")
    public ApiResponse<Csrf> csrf(CsrfToken token) {
        return ApiResponse.success(200, "Khởi tạo bảo mật thành công.", new Csrf(token.getHeaderName(), token.getToken()));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Đăng ký tài khoản")
    public ApiResponse<ProfileResponse> register(@Valid @RequestBody Registration request) {
        return ApiResponse.success(201, "Đăng ký thành công.", mapper.toResponse(accounts.register(request)));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Xem hồ sơ của chính mình")
    public ApiResponse<ProfileResponse> me(Principal principal) {
        return ApiResponse.success(200, "Lấy thông tin tài khoản thành công.", mapper.toResponse(accounts.get(UUID.fromString(principal.getName()))));
    }

    @PostMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "Cập nhật tên và số điện thoại")
    public ApiResponse<ProfileResponse> update(Principal principal, @Valid @RequestBody Profile request) {
        return ApiResponse.success(200, "Cập nhật tài khoản thành công.", mapper.toResponse(accounts.update(UUID.fromString(principal.getName()), request.fullName(), request.phone())));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Đổi refresh token và cấp JWT mới")
    public ApiResponse<AuthTokenService.TokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        return ApiResponse.success(200, "Làm mới phiên đăng nhập thành công.", tokens.refresh(request, response));
    }
}
