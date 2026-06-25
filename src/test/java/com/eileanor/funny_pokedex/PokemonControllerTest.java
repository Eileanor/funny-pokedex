package com.eileanor.funny_pokedex;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.eileanor.funny_pokedex.controller.PokemonController;
import com.eileanor.funny_pokedex.domain.PokemonResponse;
import com.eileanor.funny_pokedex.error.PokemonNotFoundException;
import com.eileanor.funny_pokedex.error.RateLimitExceededException;
import com.eileanor.funny_pokedex.service.PokemonService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(PokemonController.class)
class PokemonControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PokemonService pokemonService;

    @Test
    @DisplayName("GET /pokemon/{name} returns 200 OK with correct JSON for valid species")
    void getPokemon_returnsOk() throws Exception {
        when(pokemonService.getPokemon("mewtwo"))
                .thenReturn(PokemonResponse.builder()
                        .name("mewtwo")
                        .description("A rare Pokémon.")
                        .habitat("rare")
                        .isLegendary(true)
                        .build());

        mockMvc.perform(get("/pokemon/mewtwo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("mewtwo"))
                .andExpect(jsonPath("$.description").value("A rare Pokémon."))
                .andExpect(jsonPath("$.habitat").value("rare"));
    }

    @Test
    @DisplayName("GET /pokemon/{name} returns 404 Not Found for unknown species")
    void getPokemon_unknownPokemon_returns404() throws Exception {
        when(pokemonService.getPokemon("unknown"))
                .thenThrow(new PokemonNotFoundException("unknown"));

        mockMvc.perform(get("/pokemon/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /pokemon/translated/{name} returns 200 OK with correct JSON for valid species")
    void getTranslatedPokemon_returnsOk() throws Exception {
        when(pokemonService.getTranslatedPokemon("mewtwo"))
                .thenReturn(PokemonResponse.builder()
                        .name("mewtwo")
                        .description("Created by gene splicing, it was.")
                        .habitat("rare")
                        .isLegendary(true)
                        .build());

        mockMvc.perform(get("/pokemon/translated/mewtwo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("mewtwo"))
                .andExpect(jsonPath("$.description").value("Created by gene splicing, it was."));
    }

    @Test
    @DisplayName("GET /pokemon/translated/{name} returns 404 Not Found for unknown species")
    void getTranslatedPokemon_unknownPokemon_returns404() throws Exception {
        when(pokemonService.getTranslatedPokemon("unknown"))
                .thenThrow(new PokemonNotFoundException("unknown"));

        mockMvc.perform(get("/pokemon/translated/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /pokemon/{name} returns 429 Too Many Requests when rate limit is exceeded")
    void getPokemon_rateLimitExceeded_returns429() throws Exception {
        RateLimiter rateLimiter = RateLimiter.of("test", RateLimiterConfig.ofDefaults());
        when(pokemonService.getPokemon("mewtwo"))
                .thenThrow(RequestNotPermitted.createRequestNotPermitted(rateLimiter));

        mockMvc.perform(get("/pokemon/mewtwo"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Rate limit exceeded"));
    }

    @Test
    @DisplayName("GET /pokemon/{name} returns 200 when habitat is null in response")
    void getPokemon_nullHabitat_habitatAbsentFromJson() throws Exception {
        when(pokemonService.getPokemon("mew"))
                .thenReturn(PokemonResponse.builder()
                        .name("mew")
                        .description("So rare that it is still said to be a mirage.")
                        .isLegendary(true)
                        .build());

        mockMvc.perform(get("/pokemon/mew"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habitat").doesNotExist());
    }

    @Test
    @DisplayName("GET /pokemon/{name} returns 400 Bad Request for invalid characters in name")
    void getPokemon_invalidCharsName_returns400() throws Exception {
        mockMvc.perform(get("/pokemon/mewtwo!@#"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Pokemon name is invalid"));
    }

    @Test
    @DisplayName("GET /pokemon/{name} returns 400 Bad Request for empty name")
    void getPokemon_invalidEmptyName_returns400() throws Exception {
        mockMvc.perform(get("/pokemon/  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Pokemon name is invalid"));
    }

    @Test
    @DisplayName("GET /pokemon/{name} returns 429 Too Many Requests when rate limit is exceeded")
    void getPokemon_appRateLimitExceeded_returns429() throws Exception {
        when(pokemonService.getPokemon("mewtwo"))
                .thenThrow(new RateLimitExceededException());

        mockMvc.perform(get("/pokemon/mewtwo"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Rate limit exceeded"));
    }

    @Test
    @DisplayName("GET /pokemon/{name} returns 503 Service Unavailable when circuit breaker is open")
    void getPokemon_circuitBreakerOpen_returns503() throws Exception {
        CircuitBreaker cb = CircuitBreaker.ofDefaults("test");
        when(pokemonService.getPokemon("mewtwo"))
                .thenThrow(CallNotPermittedException.createCallNotPermittedException(cb));

        mockMvc.perform(get("/pokemon/mewtwo"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Upstream temporarily unavailable"));
    }

    @Test
    @DisplayName("GET /pokemon/{name} returns 404 Not Found for non-existing endpoints")
    void unknownUrl_returns404() throws Exception {
        mockMvc.perform(get("/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST /pokemon/{name} returns 405 Method Not Allowed")
    void postMethod_notAllowed_returns405() throws Exception {
        mockMvc.perform(post("/pokemon/mewtwo"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Method not allowed"));
    }

    @Test
    @DisplayName("GET /pokemon/{name} returns 500 Internal Server Error for unexpected exceptions")
    void getPokemon_unexpectedError_returns500() throws Exception {
        when(pokemonService.getPokemon("mewtwo"))
                .thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(get("/pokemon/mewtwo"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"));
    }
}
