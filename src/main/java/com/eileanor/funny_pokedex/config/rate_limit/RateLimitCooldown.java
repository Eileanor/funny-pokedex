package com.eileanor.funny_pokedex.config.rate_limit;

public interface RateLimitCooldown {
    boolean isRateLimited();

    void setRateLimitedFor(long seconds);
}
