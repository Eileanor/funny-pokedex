package com.eileanor.funny_pokedex.controller;

import com.eileanor.funny_pokedex.domain.PokemonResponse;
import com.eileanor.funny_pokedex.service.PokemonService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pokemon")
public class PokemonController {

    private final PokemonService pokemonService;

    public PokemonController(PokemonService pokemonService) {
        this.pokemonService = pokemonService;
    }

    @GetMapping("/{name}")
    @RateLimiter(name = "inbound")
    public PokemonResponse getPokemon(@PathVariable String name) {
        return pokemonService.getPokemon(name);
    }

    @GetMapping("/translated/{name}")
    @RateLimiter(name = "inbound")
    public PokemonResponse getTranslatedPokemon(@PathVariable String name) {
        return pokemonService.getTranslatedPokemon(name);
    }
}
