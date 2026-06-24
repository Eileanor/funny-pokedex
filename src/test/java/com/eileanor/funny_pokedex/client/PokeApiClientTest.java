package com.eileanor.funny_pokedex.client;

import com.eileanor.funny_pokedex.domain.pokeapi.PokemonSpeciesResponse;
import com.eileanor.funny_pokedex.error.PokemonNotFoundException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PokeApiClientTest {

    MockWebServer server;
    PokeApiClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        WebClient webClient = WebClient.builder()
                .baseUrl(server.url("/").toString())
                .build();
        client = new PokeApiClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void fetchSpecies_happyPath_deserializesResponse() {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "name": "mewtwo",
                          "flavor_text_entries": [
                            {"flavor_text": "A rare Pokémon.", "language": {"name": "en"}}
                          ],
                          "habitat": {"name": "rare"},
                          "is_legendary": true
                        }
                        """));

        PokemonSpeciesResponse result = client.fetchSpecies("mewtwo");

        assertThat(result.name()).isEqualTo("mewtwo");
        assertThat(result.flavorTextEntries()).hasSize(1);
        assertThat(result.flavorTextEntries().get(0).flavorText()).isEqualTo("A rare Pokémon.");
        assertThat(result.habitat().name()).isEqualTo("rare");
        assertThat(result.isLegendary()).isTrue();
    }

    @Test
    void fetchSpecies_notFound_throwsPokemonNotFoundException() {
        server.enqueue(new MockResponse().setResponseCode(404));

        assertThatThrownBy(() -> client.fetchSpecies("nonexistent"))
                .isInstanceOf(PokemonNotFoundException.class);
    }

    @Test
    void fetchSpecies_serverError_throwsWebClientResponseException() {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> client.fetchSpecies("mewtwo"))
                .isInstanceOf(WebClientResponseException.class);
    }
}
