package vn.camerahub.micro.config;

import java.time.Clock;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class AppConfig {
    @Bean
    public Clock applicationClock() {
        return Clock.systemUTC();
    }
}
