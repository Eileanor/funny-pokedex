package com.eileanor.funny_pokedex.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.eileanor.funny_pokedex.domain.pokeapi.PokemonSpeciesResponse;
import com.eileanor.funny_pokedex.error.PokemonNotFoundException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PokeApiClient {

    private final WebClient webClient;

    public PokeApiClient(@Qualifier("pokeApiWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @CircuitBreaker(name = "pokeApi")
    @Retry(name = "pokeApi")
    public PokemonSpeciesResponse fetchSpecies(String name) {
        try {
            return webClient.get()
                    .uri("/pokemon-species/{name}", name)
                    .retrieve()
                    .bodyToMono(PokemonSpeciesResponse.class)
                    .block();
        } catch (WebClientResponseException ex) {
            log.error("GET - PokeApi - {}", ex.getMessage());
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new PokemonNotFoundException(name);
            }
            throw ex;
        } catch (Exception ex) {
            log.error("GET - PokeApi - {}", ex.getMessage());
            throw ex;
        }
    }
}
