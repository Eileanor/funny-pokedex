package com.eileanor.funny_pokedex.client;

import com.eileanor.funny_pokedex.domain.funtranslations.FunTranslationRequest;
import com.eileanor.funny_pokedex.domain.funtranslations.FunTranslationResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Component
public class FunTranslationsClient {

    private final WebClient webClient;

    public FunTranslationsClient(@Qualifier("funTranslationsWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @CircuitBreaker(name = "funTranslations", fallbackMethod = "emptyFallback")
    @Retry(name = "funTranslations")
    public Optional<String> translateShakespeare(String text) {
        return translate("/translate/shakespeare", text);
    }

    @CircuitBreaker(name = "funTranslations", fallbackMethod = "emptyFallback")
    @Retry(name = "funTranslations")
    public Optional<String> translateYoda(String text) {
        return translate("/translate/yoda", text);
    }

    private Optional<String> translate(String path, String text) {
        FunTranslationResponse response = webClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new FunTranslationRequest(text))
                .retrieve()
                .bodyToMono(FunTranslationResponse.class)
                .block();

        if (response == null || response.success() == null ||
                response.success().total() == 0
                || response.contents() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(response.contents().translated());
    }

    // Fallback for both @CircuitBreaker decorators — any error returns empty so the
    // service layer can fall back to the standard description (translation rule 3).
    private Optional<String> emptyFallback(String text, Throwable t) {
        return Optional.empty();
    }
}
