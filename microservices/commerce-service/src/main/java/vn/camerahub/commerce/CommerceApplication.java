package vn.camerahub.commerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.boot.autoconfigure.domain.EntityScan("vn.camerahub.commerce.entity")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories("vn.camerahub.commerce.repository")
@SpringBootApplication(scanBasePackages = {"vn.camerahub.commerce", "vn.camerahub.micro"})
public class CommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommerceApplication.class, args);
    }
}

