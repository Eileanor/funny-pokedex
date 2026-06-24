package com.eileanor.funny_pokedex.service;

import com.eileanor.funny_pokedex.client.FunTranslationsClient;
import com.eileanor.funny_pokedex.client.PokeApiClient;
import com.eileanor.funny_pokedex.domain.PokemonResponse;
import com.eileanor.funny_pokedex.domain.pokeapi.PokemonSpeciesResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
class PokemonServiceImpl implements PokemonService {

    private final PokeApiClient pokeApiClient;
    private final FunTranslationsClient funTranslationsClient;

    PokemonServiceImpl(PokeApiClient pokeApiClient, FunTranslationsClient funTranslationsClient) {
        this.pokeApiClient = pokeApiClient;
        this.funTranslationsClient = funTranslationsClient;
    }

    @Override
    @Cacheable(value = "pokemon", key = "#name")
    public PokemonResponse getPokemon(String name) {
        PokemonSpeciesResponse species = pokeApiClient.fetchSpecies(name);
        String description = extractEnglishDescription(species);
        String habitat = species.habitat() != null ? species.habitat().name() : null;
        return new PokemonResponse(species.name(), description, habitat, species.isLegendary());
    }

    @Override
    @Cacheable(value = "pokemon-translated", key = "#name")
    public PokemonResponse getTranslatedPokemon(String name) {
        PokemonSpeciesResponse species = pokeApiClient.fetchSpecies(name);
        String standard = extractEnglishDescription(species);
        String habitat = species.habitat() != null ? species.habitat().name() : null;
        Optional<String> translated = useYoda(habitat, species.isLegendary())
                ? funTranslationsClient.translateYoda(standard)
                : funTranslationsClient.translateShakespeare(standard);

        String description = translated.orElse(standard);
        return new PokemonResponse(species.name(), description, habitat, species.isLegendary());
    }

    // Rule: cave habitat OR legendary → Yoda; everything else → Shakespeare.
    private boolean useYoda(String habitat, boolean isLegendary) {
        return isLegendary || "cave".equals(habitat);
    }

    // Pick the first English flavor text and sanitize control characters embedded
    // by PokéAPI.
    private String extractEnglishDescription(PokemonSpeciesResponse species) {
        return species.flavorTextEntries().stream()
                .filter(e -> e.language() != null && "en".equals(e.language().name()))
                .map(e -> e.flavorText().replaceAll("[\\f\\n\\r\\t]", " ").trim())
                .findFirst()
                .orElse("");
    }
}
