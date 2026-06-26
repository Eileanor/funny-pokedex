package com.eileanor.funny_pokedex.config.rate_limit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.rate-limit.inbound")
public record RateLimitProperties(int limitForPeriod, Duration limitRefreshPeriod, Duration timeoutDuration) {
}
