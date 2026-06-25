package com.eileanor.funny_pokedex.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PokemonResponse(
        String name,
        String description,
        String habitat, // null for species with no habitat — omitted from JSON
        boolean isLegendary) {
}
