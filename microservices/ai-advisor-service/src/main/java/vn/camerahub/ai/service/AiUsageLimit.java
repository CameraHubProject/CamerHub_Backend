package vn.camerahub.ai.service;

import vn.camerahub.ai.exception.*;

import java.time.*;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import vn.camerahub.ai.repository.AiUsageRepository;

@Service
@lombok.RequiredArgsConstructor
public class AiUsageLimit {
    private final AiUsageRepository repository;
    private final AiSettings settings;

    @Transactional
    public void acquire(String client) {
        var day = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        long minute = Instant.now().getEpochSecond() / 60;
        String key;
        try {
            key = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(client.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        repository.lockBudget();
        if (repository.daily(day) >= settings.dailyLimit() || repository.minute(key, minute) >= 6)
            throw new AdvisorException(AdvisorErrorCode.DAILY_LIMIT_REACHED);
        repository.increment(day, key, minute);
    }
}
