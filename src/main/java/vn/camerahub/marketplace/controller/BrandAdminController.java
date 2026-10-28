package vn.camerahub.marketplace.controller;

import vn.camerahub.marketplace.service.BrandAdminService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

import org.springframework.web.bind.annotation.*;

@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin/brands")
@lombok.RequiredArgsConstructor
public class BrandAdminController {
    private final BrandAdminService service;

    @GetMapping
    public Object list() {
        return service.list();
    }

    @PostMapping
    @vn.camerahub.shared.dto.SuccessMessage("Đã thêm hãng máy. Người bán có thể chọn hãng này khi đăng tin.")
    public Object create(@Valid @RequestBody BrandName input) {
        return service.save(null, input.name());
    }

    @PostMapping("/{id}")
    @vn.camerahub.shared.dto.SuccessMessage("Đã cập nhật tên hãng máy.")
    public Object update(@PathVariable UUID id, @Valid @RequestBody BrandName input) {
        return service.save(id, input.name());
    }

    @PostMapping("/{id}/delete")
    public vn.camerahub.shared.dto.ApiResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);

        return vn.camerahub.shared.dto.ApiResponse.success(200, "Đã xóa hãng máy khỏi danh sách.", null);
    }

    public record BrandName(@NotBlank(message = "Vui lòng nhập tên hãng máy.")
                            @Size(max = 100, message = "Tên hãng máy không được vượt quá 100 ký tự.") String name) {
    }
}

