package com.eileanor.funny_pokedex.config.rate_limit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisRateLimitCooldownTest {

    private static final String KEY = "funtranslations:rate-limited";

    StringRedisTemplate redisTemplate;
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    RedisRateLimitCooldown cooldown;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        cooldown = new RedisRateLimitCooldown(redisTemplate);
    }

    @Test
    @DisplayName("isRateLimited returns false when key is absent in Redis")
    void isRateLimited_returnsFalse_whenKeyAbsent() {
        when(redisTemplate.hasKey(KEY)).thenReturn(false);
        assertThat(cooldown.isRateLimited()).isFalse();
    }

    @Test
    @DisplayName("isRateLimited returns true when key is present in Redis")
    void isRateLimited_returnsTrue_whenKeyPresent() {
        when(redisTemplate.hasKey(KEY)).thenReturn(true);
        assertThat(cooldown.isRateLimited()).isTrue();
    }

    @Test
    @DisplayName("isRateLimited fails open when Redis throws — allows call through")
    void isRateLimited_failsOpen_whenRedisThrows() {
        when(redisTemplate.hasKey(KEY)).thenThrow(new RuntimeException("Redis down"));
        assertThat(cooldown.isRateLimited()).isFalse();
    }

    @Test
    @DisplayName("setRateLimitedFor writes the key with the exact TTL from retry_after")
    void setRateLimitedFor_writesKeyWithCorrectTtl() {
        cooldown.setRateLimitedFor(42);
        verify(valueOps).set(eq(KEY), eq("1"), eq(Duration.ofSeconds(42)));
    }

    @Test
    @DisplayName("setRateLimitedFor does not throw when Redis write fails")
    void setRateLimitedFor_doesNotThrow_whenRedisWriteFails() {
        doThrow(new RuntimeException("Redis connection error"))
                .when(valueOps).set(anyString(), anyString(), any(Duration.class));
        assertThatCode(() -> cooldown.setRateLimitedFor(42)).doesNotThrowAnyException();
    }
}
