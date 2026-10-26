package vn.camerahub.ai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class AiSettings {
    private final String key, model;
    private final boolean enabled, web;
    private final int dailyLimit;

    public AiSettings(@Value("${camerahub.ai.api-key}") String key, @Value("${camerahub.ai.model}") String model,
                      @Value("${camerahub.ai.enabled}") boolean enabled, @Value("${camerahub.ai.web-search}") boolean web,
                      @Value("${camerahub.ai.daily-limit}") int dailyLimit) {
        this.key = key.strip();
        this.model = model;
        this.enabled = enabled;
        this.web = web;
        this.dailyLimit = Math.max(0, dailyLimit);
    }

    public boolean enabled() {
        return enabled && !key.isBlank();
    }

    public String key() {
        return key;
    }

    public String model() {
        return model;
    }

    public boolean web() {
        return web;
    }

    public int dailyLimit() {
        return dailyLimit;
    }
}

