package vn.camerahub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan({"vn.camerahub.identity.entity", "vn.camerahub.marketplace.entity", "vn.camerahub.moderation.entity", "vn.camerahub.commerce.entity", "vn.camerahub.ai.entity"})
@EnableJpaRepositories({"vn.camerahub.identity.repository", "vn.camerahub.marketplace.repository", "vn.camerahub.moderation.repository", "vn.camerahub.commerce.repository", "vn.camerahub.ai.repository"})
public class CameraHubApplication {
    public static void main(String[] args) { SpringApplication.run(CameraHubApplication.class, args); }
}
