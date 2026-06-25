package com.eileanor.funny_pokedex.controller;

import java.util.Locale;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eileanor.funny_pokedex.domain.PokemonResponse;
import com.eileanor.funny_pokedex.service.PokemonService;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/pokemon")
@Slf4j
public class PokemonController {

    private final PokemonService pokemonService;

    public PokemonController(PokemonService pokemonService) {
        this.pokemonService = pokemonService;
    }

    @GetMapping("/{name}")
    @RateLimiter(name = "inbound")
    public PokemonResponse getPokemon(@PathVariable String name) {
        return pokemonService.getPokemon(sanitizeName(name));
    }

    @GetMapping("/translated/{name}")
    @RateLimiter(name = "inbound")
    public PokemonResponse getTranslatedPokemon(@PathVariable String name) {
        return pokemonService.getTranslatedPokemon(sanitizeName(name));
    }

    private String sanitizeName(String name) {
        String trimmed = name.trim();
        if (trimmed.isEmpty() || !trimmed.matches("[\\p{L}\\p{M}a-zA-Z0-9\\-]+")) {
            throw new IllegalArgumentException("Pokemon name is invalid");
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
