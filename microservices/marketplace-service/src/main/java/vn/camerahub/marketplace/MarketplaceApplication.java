package vn.camerahub.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.boot.autoconfigure.domain.EntityScan("vn.camerahub.marketplace.entity")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories("vn.camerahub.marketplace.repository")
@SpringBootApplication(scanBasePackages = {"vn.camerahub.marketplace", "vn.camerahub.micro"})
public class MarketplaceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MarketplaceApplication.class, args);
    }
}

