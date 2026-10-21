package vn.camerahub.health;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/health")
    public Health health() {
        jdbc.queryForObject("SELECT 1", Integer.class);
        return new Health("UP", "CameraHub", "SIMULATED");
    }

    public record Health(String status, String service, String moneyMode) {}
}
