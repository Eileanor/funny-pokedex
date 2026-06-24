package com.eileanor.funny_pokedex.error;

public class PokemonNotFoundException extends RuntimeException {
    public PokemonNotFoundException(String name) {
        super("Pokemon '" + name + "' not found");
    }
}
