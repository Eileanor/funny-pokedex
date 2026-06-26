package com.eileanor.funny_pokedex.config.cache;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.cache")
public record CacheProperties(Duration ttl, CacheName name) {

    public record CacheName(String pokemon, String pokemonTranslated) {}

    public List<String> cacheNames() {
        return List.of(name.pokemon(), name.pokemonTranslated());
    }
}