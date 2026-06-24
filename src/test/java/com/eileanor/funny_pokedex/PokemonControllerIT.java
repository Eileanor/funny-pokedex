package com.eileanor.funny_pokedex;

import com.eileanor.funny_pokedex.client.FunTranslationsClient;
import com.eileanor.funny_pokedex.client.PokeApiClient;
import com.eileanor.funny_pokedex.domain.PokemonResponse;
import com.eileanor.funny_pokedex.domain.pokeapi.FlavorTextEntry;
import com.eileanor.funny_pokedex.domain.pokeapi.NamedResource;
import com.eileanor.funny_pokedex.domain.pokeapi.PokemonSpeciesResponse;
import com.eileanor.funny_pokedex.error.PokemonNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("prod")
@Testcontainers
class PokemonControllerIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @MockitoBean
    PokeApiClient pokeApiClient;
    @MockitoBean
    FunTranslationsClient funTranslationsClient;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void getPokemon_returnsCorrectResponse() {
        when(pokeApiClient.fetchSpecies("mewtwo")).thenReturn(mewtwoSpecies());

        ResponseEntity<PokemonResponse> response = restTemplate.getForEntity("/pokemon/mewtwo", PokemonResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("mewtwo");
        assertThat(response.getBody().isLegendary()).isTrue();
    }

    @Test
    void getPokemon_notFound_returns404() {
        when(pokeApiClient.fetchSpecies("notreal")).thenThrow(new PokemonNotFoundException("notreal"));

        ResponseEntity<String> response = restTemplate.getForEntity("/pokemon/notreal", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getTranslatedPokemon_returnsTranslatedDescription() {
        when(pokeApiClient.fetchSpecies("mewtwo")).thenReturn(mewtwoSpecies());
        when(funTranslationsClient.translateYoda(any())).thenReturn(Optional.of("Created by gene splicing, it was."));

        ResponseEntity<PokemonResponse> response = restTemplate.getForEntity("/pokemon/translated/mewtwo",
                PokemonResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().description()).isEqualTo("Created by gene splicing, it was.");
    }

    @Test
    void getPokemon_secondCallServedFromCache() {
        when(pokeApiClient.fetchSpecies("mewtwo")).thenReturn(mewtwoSpecies());

        restTemplate.getForEntity("/pokemon/mewtwo", PokemonResponse.class);
        restTemplate.getForEntity("/pokemon/mewtwo", PokemonResponse.class);

        // Cache should have served the second request — PokeApiClient called only once.
        verify(pokeApiClient, times(1)).fetchSpecies("mewtwo");
    }

    private static PokemonSpeciesResponse mewtwoSpecies() {
        var entry = new FlavorTextEntry("Created by gene splicing.", new NamedResource("en"));
        return new PokemonSpeciesResponse("mewtwo", List.of(entry), new NamedResource("rare"), true);
    }
}
