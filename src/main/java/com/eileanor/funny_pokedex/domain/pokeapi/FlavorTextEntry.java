package com.eileanor.funny_pokedex.domain.pokeapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FlavorTextEntry(
        @JsonProperty("flavor_text") String flavorText,
        NamedResource language) {
}
