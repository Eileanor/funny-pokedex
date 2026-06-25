package com.eileanor.funny_pokedex.client;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.eileanor.funny_pokedex.config.FunTranslationsProperties;
import com.eileanor.funny_pokedex.domain.funtranslations.FunTranslationRequest;
import com.eileanor.funny_pokedex.domain.funtranslations.FunTranslationResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class FunTranslationsClient {

    private final WebClient webClient;
    private final String shakespearePath;
    private final String yodaPath;

    public FunTranslationsClient(
            @Qualifier("funTranslationsWebClient") WebClient webClient,
            FunTranslationsProperties properties) {
        this.webClient = webClient;
        this.shakespearePath = properties.shakespearePath();
        this.yodaPath = properties.yodaPath();
    }

    @CircuitBreaker(name = "funTranslations", fallbackMethod = "emptyFallback")
    @Retry(name = "funTranslations")
    public Optional<String> translateShakespeare(String text) {
        return translate(shakespearePath, text);
    }

    @CircuitBreaker(name = "funTranslations", fallbackMethod = "emptyFallback")
    @Retry(name = "funTranslations")
    public Optional<String> translateYoda(String text) {
        return translate(yodaPath, text);
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
            log.warn("POST - /translated{} - Failed to translate text", path);
            return Optional.empty();
        }
        log.info("POST - /translated{} - Successfully translated text", path);
        return Optional.ofNullable(response.contents().translated());
    }

    // Fallback for both @CircuitBreaker decorators — any error returns empty so the
    // service layer can fall back to the standard description (translation rule 3).
    private Optional<String> emptyFallback(String text, Throwable t) {
        log.warn("POST - /translated - FALLBACK");
        return Optional.empty();
    }
}
