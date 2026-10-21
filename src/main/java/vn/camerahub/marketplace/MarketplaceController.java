package vn.camerahub.marketplace;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class MarketplaceController {
    private final MarketplaceService service;
    public MarketplaceController(MarketplaceService service) { this.service=service; }
    private UUID user(Principal principal) { return UUID.fromString(principal.getName()); }
    @GetMapping("/api/public/products") public Map<String,Object> products() { return Map.of("items",service.products()); }
    @GetMapping("/api/public/products/{id}") public Map<String,Object> product(@PathVariable UUID id) { return service.detail(id); }
    @GetMapping("/api/public/models") public Object models() { return service.models(); }
    @GetMapping("/api/public/brands") public Object brands() { return service.brands(); }
    @GetMapping("/api/public/showrooms") public Object showrooms() { return service.showrooms(); }
    @GetMapping("/api/marketplace/addresses") public Object addresses(Principal p) { return service.addresses(user(p)); }
    @PostMapping("/api/marketplace/addresses") public Object address(Principal p,@Valid @RequestBody Address input) { return service.address(user(p),input); }
    @GetMapping("/api/marketplace/listings") public Object listings(Principal p) { return service.listings(user(p)); }
    @PostMapping("/api/marketplace/listings") public Object listing(Principal p,@Valid @RequestBody Listing input) { return service.listing(user(p),input); }
    @GetMapping("/api/admin/listings") public Object reviews() { return service.reviews(); }
    @PostMapping("/api/admin/listings/{id}/review") public void review(Principal p,@PathVariable UUID id,@Valid @RequestBody Review input) { service.review(user(p),id,input); }
    @PostMapping("/api/marketplace/checkout") public Object checkout(Principal p,@Valid @RequestBody Checkout input) { return service.checkout(user(p),input); }
    @GetMapping("/api/marketplace/orders") public Object orders(Principal p) { return service.orders(user(p)); }
    @GetMapping("/api/marketplace/orders/{id}") public Object order(Principal p,@PathVariable UUID id) { return service.order(user(p),id); }
    @PostMapping("/api/marketplace/orders/{id}/pay") public Object pay(Principal p,@PathVariable UUID id,@Valid @RequestBody Payment input) { return service.pay(user(p),id,input.stage()); }
    @PostMapping("/api/marketplace/orders/{id}/cancel") public void cancel(Principal p,@PathVariable UUID id) { service.cancelUnpaid(user(p),id); }
    @GetMapping("/api/marketplace/buyback") public Object buybacks(Principal p) { return service.buybacks(user(p)); }
    @PostMapping("/api/marketplace/buyback") public Object buyback(Principal p,@Valid @RequestBody Buyback input) { return service.buyback(user(p),input); }
    public record Address(@NotBlank @Size(max=160) String recipientName,@NotBlank @Pattern(regexp="[+0-9 ()-]{8,20}") String phone,
        @NotBlank @Size(max=160) String province,@NotBlank @Size(max=500) String streetAddress,boolean isDefault) {}
    public record Checkout(@NotNull UUID requestId,@NotEmpty @Size(max=20) List<@NotNull UUID> productIds,
        @NotBlank @Pattern(regexp="DELIVERY|SHOWROOM") String delivery,@NotNull @Valid Address address) {}
    public record Payment(@NotBlank @Pattern(regexp="DEPOSIT|BALANCE_AND_SHIPPING") String stage) {}
    public record Listing(UUID modelId,@NotBlank @Size(max=240) String title,@NotBlank String priceVnd,
        @Min(0) Integer shutterCount,@NotBlank @Size(max=10000) String description,@NotBlank @Size(max=4000) String conditionNotes,
        @Size(max=2000) String accessories,@Size(max=200) String serialNumber,
        @NotNull @Size(max=4) List<@Pattern(regexp="TRAVEL|VLOG|PRO|BEGINNER") String> purposes,
        UUID brandId,@Pattern(regexp="BODY|LENS|OTHER") String deviceType) {
        @AssertTrue(message="Chọn hãng hoặc model, không gửi cả hai.")
        public boolean isSelectionValid() { return (modelId==null)!=(brandId==null); }
        // Preserve the existing Java/API model-based contract for older clients.
        public Listing(UUID modelId,String title,String priceVnd,Integer shutterCount,String description,
                String conditionNotes,String accessories,String serialNumber,List<String> purposes) {
            this(modelId,title,priceVnd,shutterCount,description,conditionNotes,accessories,serialNumber,purposes,null,null);
        }
    }
    public record Review(boolean approve,@NotBlank @Size(max=2000) String note) {}
    public record Buyback(@NotBlank @Size(max=160) String model,@NotBlank @Size(max=4000) String conditionNotes,String expectedPriceVnd) {}
}
