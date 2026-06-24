package com.eileanor.funny_pokedex.error;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() {
        super("Rate limit exceeded");
    }
}
