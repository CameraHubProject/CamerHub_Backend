package vn.camerahub.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.boot.autoconfigure.domain.EntityScan("vn.camerahub.ai.entity")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories("vn.camerahub.ai.repository")
@SpringBootApplication(scanBasePackages = {"vn.camerahub.ai", "vn.camerahub.micro"})
public class AiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}

