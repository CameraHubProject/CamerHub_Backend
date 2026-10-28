package vn.camerahub.commerce.controller;

import vn.camerahub.commerce.service.CheckoutSaga;
import vn.camerahub.commerce.service.CommerceService;
import vn.camerahub.commerce.dto.CommerceDtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.security.Principal;
import java.util.*;

import org.springframework.web.bind.annotation.*;

@RestController
@lombok.RequiredArgsConstructor
public class CommerceController {
    private final CommerceService service;
    private final CheckoutSaga checkout;

    private UUID user(Principal p) {
        return UUID.fromString(p.getName());
    }

    @GetMapping("/api/public/showrooms")
    public Object showrooms() {
        return checkout.showrooms();
    }

    @PostMapping("/api/marketplace/checkout")
    @vn.camerahub.shared.dto.SuccessMessage("Đã tạo đơn hàng và giữ thiết bị cho bạn.")
    public Object checkout(Principal p, @Valid @RequestBody Checkout input) {
        return checkout.checkout(user(p), input);
    }

    @GetMapping("/api/marketplace/orders")
    public Object orders(Principal p) {
        return service.orders(user(p));
    }

    @GetMapping("/api/marketplace/orders/{id}")
    public Object order(Principal p, @PathVariable UUID id) {
        return service.order(user(p), id);
    }

    @PostMapping("/api/marketplace/orders/{id}/pay")
    @vn.camerahub.shared.dto.SuccessMessage("Đã ghi nhận thanh toán mô phỏng.")
    public Object pay(Principal p, @PathVariable UUID id, @Valid @RequestBody Payment input) {
        return service.pay(user(p), id, input.stage());
    }

    @PostMapping("/api/marketplace/orders/{id}/cancel")
    public vn.camerahub.shared.dto.ApiResponse<Void> cancel(Principal p, @PathVariable UUID id) {
        checkout.cancel(user(p), id);

        return vn.camerahub.shared.dto.ApiResponse.success(200, "Đã hủy đơn hàng.", null);
    }

    @GetMapping("/api/marketplace/buyback")
    public Object buybacks(Principal p) {
        return service.buybacks(user(p));
    }

    @PostMapping("/api/marketplace/buyback")
    @vn.camerahub.shared.dto.SuccessMessage("Đã tiếp nhận yêu cầu. Bạn có thể theo dõi trạng thái bên dưới.")
    public Object buyback(Principal p, @Valid @RequestBody Buyback input) {
        return service.buyback(user(p), input);
    }

}

