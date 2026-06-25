package com.eileanor.funny_pokedex.domain.pokeapi;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

import java.util.List;

@Builder
public record PokemonSpeciesResponse(
        String name,
        @JsonProperty("flavor_text_entries") List<FlavorTextEntry> flavorTextEntries,
        NamedResource habitat, // nullable — some species have no habitat
        @JsonProperty("is_legendary") boolean isLegendary) {
}
