package vn.camerahub.micro;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
public class ServiceHealth {
    private final JdbcTemplate jdbc;
    private final String name;

    public ServiceHealth(JdbcTemplate jdbc, @Value("${spring.application.name}") String name) {
        this.jdbc = jdbc;
        this.name = name;
    }

    @GetMapping("/api/health")
    public Object health() {
        jdbc.queryForObject("SELECT 1", Integer.class);
        return Map.of("status", "UP", "service", name);
    }
}

