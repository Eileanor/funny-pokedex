package com.eileanor.funny_pokedex.config.rate_limit;

import java.time.Instant;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryRateLimitCooldown implements RateLimitCooldown {

    private volatile Instant rateLimitedUntil = Instant.MIN;

    @Override
    public boolean isRateLimited() {
        return Instant.now().isBefore(rateLimitedUntil);
    }

    @Override
    public void setRateLimitedFor(long seconds) {
        rateLimitedUntil = Instant.now().plusSeconds(seconds);
    }
}
