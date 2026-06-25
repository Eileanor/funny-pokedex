package com.eileanor.funny_pokedex.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.cache")
public record CacheProperties(Duration ttl, List<String> cacheNames) {
}