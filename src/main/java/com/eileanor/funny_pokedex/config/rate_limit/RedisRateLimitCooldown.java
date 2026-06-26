package com.eileanor.funny_pokedex.config.rate_limit;

import java.time.Duration;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Profile("prod")
@Slf4j
public class RedisRateLimitCooldown implements RateLimitCooldown {

    private static final String KEY = "funtranslations:rate-limited";

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimitCooldown(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isRateLimited() {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY));
        } catch (Exception e) {
            // Redis down — let the call through; the CB and fallback handle upstream
            // errors.
            log.warn("Redis check failed, allowing upstream call: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void setRateLimitedFor(long seconds) {
        try {
            redisTemplate.opsForValue().set(KEY, "1", Duration.ofSeconds(seconds));
        } catch (Exception e) {
            log.warn("Redis write failed, cooldown will not propagate cluster-wide: {}", e.getMessage());
        }
    }
}
