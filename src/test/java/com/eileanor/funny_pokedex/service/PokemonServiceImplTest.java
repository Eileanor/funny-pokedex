package com.eileanor.funny_pokedex.service;

import com.eileanor.funny_pokedex.client.FunTranslationsClient;
import com.eileanor.funny_pokedex.client.PokeApiClient;
import com.eileanor.funny_pokedex.domain.PokemonResponse;
import com.eileanor.funny_pokedex.domain.pokeapi.FlavorTextEntry;
import com.eileanor.funny_pokedex.domain.pokeapi.NamedResource;
import com.eileanor.funny_pokedex.domain.pokeapi.PokemonSpeciesResponse;
import com.eileanor.funny_pokedex.error.PokemonNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PokemonServiceImplTest {

    @Mock
    PokeApiClient pokeApiClient;
    @Mock
    FunTranslationsClient funTranslationsClient;

    PokemonServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PokemonServiceImpl(pokeApiClient, funTranslationsClient);
    }

    @Test
    void getPokemon_returnsCorrectResponse() {
        var species = species("mewtwo", "A Pokémon.", "rare", true);
        when(pokeApiClient.fetchSpecies("mewtwo")).thenReturn(species);

        PokemonResponse result = service.getPokemon("mewtwo");

        assertThat(result.name()).isEqualTo("mewtwo");
        assertThat(result.description()).isEqualTo("A Pokémon.");
        assertThat(result.habitat()).isEqualTo("rare");
        assertThat(result.isLegendary()).isTrue();
    }

    @Test
    void getPokemon_sanitizesControlCharsInDescription() {
        var species = species("bulbasaur", "A strange\nseed was\fplanted\ron its\tback.", "grassland", false);
        when(pokeApiClient.fetchSpecies("bulbasaur")).thenReturn(species);

        PokemonResponse result = service.getPokemon("bulbasaur");

        assertThat(result.description()).isEqualTo("A strange seed was planted on its back.");
    }

    @Test
    void getPokemon_nullHabitatProducesNullInResponse() {
        var entry = new FlavorTextEntry("Some text.", new NamedResource("en"));
        var species = new PokemonSpeciesResponse("unknown-species", List.of(entry), null, false);
        when(pokeApiClient.fetchSpecies("unknown-species")).thenReturn(species);

        PokemonResponse result = service.getPokemon("unknown-species");

        assertThat(result.habitat()).isNull();
    }

    @Test
    void getPokemon_propagatesNotFoundException() {
        when(pokeApiClient.fetchSpecies("notreal")).thenThrow(new PokemonNotFoundException("notreal"));

        assertThatThrownBy(() -> service.getPokemon("notreal"))
                .isInstanceOf(PokemonNotFoundException.class);
    }

    @Test
    void getTranslatedPokemon_caveHabitatUsesYoda() {
        var species = species("zubat", "It has no eyes.", "cave", false);
        when(pokeApiClient.fetchSpecies("zubat")).thenReturn(species);
        when(funTranslationsClient.translateYoda("It has no eyes.")).thenReturn(Optional.of("No eyes, it has."));

        PokemonResponse result = service.getTranslatedPokemon("zubat");

        assertThat(result.description()).isEqualTo("No eyes, it has.");
        verify(funTranslationsClient).translateYoda("It has no eyes.");
        verify(funTranslationsClient, never()).translateShakespeare(any());
    }

    @Test
    void getTranslatedPokemon_legendaryUsesYoda() {
        var species = species("mewtwo", "Created by gene splicing.", "rare", true);
        when(pokeApiClient.fetchSpecies("mewtwo")).thenReturn(species);
        when(funTranslationsClient.translateYoda("Created by gene splicing."))
                .thenReturn(Optional.of("Created by gene splicing, it was."));

        PokemonResponse result = service.getTranslatedPokemon("mewtwo");

        assertThat(result.description()).isEqualTo("Created by gene splicing, it was.");
        verify(funTranslationsClient).translateYoda("Created by gene splicing.");
        verify(funTranslationsClient, never()).translateShakespeare(any());
    }

    @Test
    void getTranslatedPokemon_ordinaryUsesShakespeare() {
        var species = species("bulbasaur", "A strange seed.", "grassland", false);
        when(pokeApiClient.fetchSpecies("bulbasaur")).thenReturn(species);
        when(funTranslationsClient.translateShakespeare("A strange seed."))
                .thenReturn(Optional.of("A strange seed, verily."));

        PokemonResponse result = service.getTranslatedPokemon("bulbasaur");

        assertThat(result.description()).isEqualTo("A strange seed, verily.");
        verify(funTranslationsClient).translateShakespeare("A strange seed.");
        verify(funTranslationsClient, never()).translateYoda(any());
    }

    @Test
    void getTranslatedPokemon_translationUnavailableFallsBackToStandard() {
        var species = species("bulbasaur", "A strange seed.", "grassland", false);
        when(pokeApiClient.fetchSpecies("bulbasaur")).thenReturn(species);
        when(funTranslationsClient.translateShakespeare("A strange seed.")).thenReturn(Optional.empty());

        PokemonResponse result = service.getTranslatedPokemon("bulbasaur");

        assertThat(result.description()).isEqualTo("A strange seed.");
    }

    private static PokemonSpeciesResponse species(String name, String description, String habitat, boolean legendary) {
        var entry = new FlavorTextEntry(description, new NamedResource("en"));
        return new PokemonSpeciesResponse(name, List.of(entry), new NamedResource(habitat), legendary);
    }
}
