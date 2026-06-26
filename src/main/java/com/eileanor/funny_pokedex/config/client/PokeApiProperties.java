package com.eileanor.funny_pokedex.config.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.pokeapi")
public record PokeApiProperties(String baseUrl) {
}
