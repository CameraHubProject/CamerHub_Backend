package vn.camerahub.moderation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.boot.autoconfigure.domain.EntityScan("vn.camerahub.moderation.entity")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories("vn.camerahub.moderation.repository")
@SpringBootApplication(scanBasePackages = {"vn.camerahub.moderation", "vn.camerahub.micro"})
public class ModerationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModerationApplication.class, args);
    }
}

