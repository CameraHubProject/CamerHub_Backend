package vn.camerahub.identity;

import vn.camerahub.identity.entity.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.boot.autoconfigure.domain.EntityScan("vn.camerahub.identity.entity")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories("vn.camerahub.identity.repository")
@SpringBootApplication(scanBasePackages = {"vn.camerahub.identity", "vn.camerahub.micro"})
public class IdentityApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}

