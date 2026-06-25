package com.eileanor.funny_pokedex.service;

import com.eileanor.funny_pokedex.client.FunTranslationsClient;
import com.eileanor.funny_pokedex.client.PokeApiClient;
import com.eileanor.funny_pokedex.domain.PokemonResponse;
import com.eileanor.funny_pokedex.domain.pokeapi.FlavorTextEntry;
import com.eileanor.funny_pokedex.domain.pokeapi.NamedResource;
import com.eileanor.funny_pokedex.domain.pokeapi.PokemonSpeciesResponse;
import com.eileanor.funny_pokedex.error.PokemonNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @DisplayName("getPokemon returns correct response for valid species")
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
    @DisplayName("getPokemon sanitizes control characters in description")
    void getPokemon_sanitizesControlCharsInDescription() {
        var species = species("bulbasaur", "A strange\nseed was\fplanted\ron its\tback.", "grassland", false);
        when(pokeApiClient.fetchSpecies("bulbasaur")).thenReturn(species);

        PokemonResponse result = service.getPokemon("bulbasaur");

        assertThat(result.description()).isEqualTo("A strange seed was planted on its back.");
    }

    @Test
    @DisplayName("getPokemon returns null habitat in response for species with no habitat")
    void getPokemon_nullHabitatProducesNullInResponse() {
        var entry = new FlavorTextEntry("Some text.", new NamedResource("en"));
        var species = new PokemonSpeciesResponse("unknown-species", List.of(entry), null, false);
        when(pokeApiClient.fetchSpecies("unknown-species")).thenReturn(species);

        PokemonResponse result = service.getPokemon("unknown-species");

        assertThat(result.habitat()).isNull();
    }

    @Test
    @DisplayName("getPokemon propagates NotFoundException")
    void getPokemon_propagatesNotFoundException() {
        when(pokeApiClient.fetchSpecies("notreal")).thenThrow(new PokemonNotFoundException("notreal"));

        assertThatThrownBy(() -> service.getPokemon("notreal"))
                .isInstanceOf(PokemonNotFoundException.class);
    }

    @Test
    @DisplayName("getTranslatedPokemon with cave habitat uses Yoda translation")
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
    @DisplayName("getTranslatedPokemon with null habitat uses Shakespeare translation")
    void getTranslatedPokemon_nullHabitatUsesShakespeare() {
        var species = new PokemonSpeciesResponse("pikachu",
                List.of(new FlavorTextEntry("A strange seed.", new NamedResource("en"))), null, false);
        when(pokeApiClient.fetchSpecies("pikachu")).thenReturn(species);
        when(funTranslationsClient.translateShakespeare("A strange seed."))
                .thenReturn(Optional.of("A strange seed, verily."));

        PokemonResponse result = service.getTranslatedPokemon("pikachu");

        assertThat(result.description()).isEqualTo("A strange seed, verily.");
        verify(funTranslationsClient).translateShakespeare("A strange seed.");
        verify(funTranslationsClient, never()).translateYoda(any());
    }

    @Test
    @DisplayName("getTranslatedPokemon with legendary species uses Yoda translation")
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
    @DisplayName("getTranslatedPokemon with ordinary species uses Shakespeare translation")
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
    @DisplayName("getTranslatedPokemon translation unavailable falls back to standard description")
    void getTranslatedPokemon_translationUnavailableFallsBackToStandard() {
        var species = species("bulbasaur", "A strange seed.", "grassland", false);
        when(pokeApiClient.fetchSpecies("bulbasaur")).thenReturn(species);
        when(funTranslationsClient.translateShakespeare("A strange seed.")).thenReturn(Optional.empty());

        PokemonResponse result = service.getTranslatedPokemon("bulbasaur");

        assertThat(result.description()).isEqualTo("A strange seed.");
    }

    @Test
    @DisplayName("getTranslatedPokemon with legendary and cave habitat uses Yoda translation")
    void getTranslatedPokemon_legendaryAndCaveUsesYoda() {
        var species = species("mewtwo", "Created by gene splicing.", "cave", true);
        when(pokeApiClient.fetchSpecies("mewtwo")).thenReturn(species);
        when(funTranslationsClient.translateYoda("Created by gene splicing."))
                .thenReturn(Optional.of("Created by gene splicing, it was."));

        PokemonResponse result = service.getTranslatedPokemon("mewtwo");

        assertThat(result.description()).isEqualTo("Created by gene splicing, it was.");
        verify(funTranslationsClient).translateYoda("Created by gene splicing.");
        verify(funTranslationsClient, never()).translateShakespeare(any());
    }

    @Test
    @DisplayName("getTranslatedPokemon propagates NotFoundException")
    void getTranslatedPokemon_propagatesNotFoundException() {
        when(pokeApiClient.fetchSpecies("notreal")).thenThrow(new PokemonNotFoundException("notreal"));

        assertThatThrownBy(() -> service.getTranslatedPokemon("notreal"))
                .isInstanceOf(PokemonNotFoundException.class);
    }

    @Test
    @DisplayName("getTranslatedPokemon handles non-english species gracefully")
    void getTranslated_Pokemon_nonEnglishSpecies() {
        var entry = new FlavorTextEntry("Un Pokémon.", new NamedResource("fr"));
        var species = new PokemonSpeciesResponse("pikachu", List.of(entry), new NamedResource("forest"), false);
        when(pokeApiClient.fetchSpecies("pikachu")).thenReturn(species);

        PokemonResponse result = service.getTranslatedPokemon("pikachu");

        assertThat(result.description()).isEqualTo("");
    }

    @Test
    @DisplayName("getPokemon ignores flavor text entries with null language")
    void getPokemon_ignoresNullLanguageEntry() {
        var entry = new FlavorTextEntry("No language.", null);
        var species = new PokemonSpeciesResponse("mew", List.of(entry), new NamedResource("cave"), false);
        when(pokeApiClient.fetchSpecies("mew")).thenReturn(species);

        PokemonResponse result = service.getPokemon("mew");

        assertThat(result.description()).isEmpty();
    }

    private static PokemonSpeciesResponse species(String name, String description, String habitat, boolean legendary) {
        var entry = new FlavorTextEntry(description, new NamedResource("en"));
        return new PokemonSpeciesResponse(name, List.of(entry), new NamedResource(habitat), legendary);
    }
}
