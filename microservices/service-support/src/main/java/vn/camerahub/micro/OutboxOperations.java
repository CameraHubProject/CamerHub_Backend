package vn.camerahub.micro;

import java.util.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@lombok.RequiredArgsConstructor
public class OutboxOperations {
    private final JdbcTemplate jdbc;

    @GetMapping("/internal/outbox")
    public Object status() {
        return jdbc.queryForMap("SELECT count(*) FILTER(WHERE delivered_at IS NULL) AS pending,count(*) FILTER(WHERE delivered_at IS NULL AND attempts>=12) AS failed FROM camerahub.integration_outbox");
    }

    @PostMapping("/internal/outbox/{id}/retry")
    public Object retry(@PathVariable UUID id) {
        jdbc.update("UPDATE camerahub.integration_outbox SET attempts=0,next_attempt_at=now() WHERE event_id=? AND delivered_at IS NULL AND (leased_until IS NULL OR leased_until<now())", id);
        return Map.of("accepted", true);
    }
}

