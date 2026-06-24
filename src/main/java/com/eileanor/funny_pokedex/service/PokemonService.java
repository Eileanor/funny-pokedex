package com.eileanor.funny_pokedex.service;

import com.eileanor.funny_pokedex.domain.PokemonResponse;

public interface PokemonService {

    PokemonResponse getPokemon(String name);

    PokemonResponse getTranslatedPokemon(String name);
}
