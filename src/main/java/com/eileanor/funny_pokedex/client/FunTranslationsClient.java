package com.eileanor.funny_pokedex.client;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.eileanor.funny_pokedex.config.rate_limit.RateLimitCooldown;
import com.eileanor.funny_pokedex.config.client.FunTranslationsProperties;
import com.eileanor.funny_pokedex.domain.funtranslations.FunTranslationErrorResponse;
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
    private final RateLimitCooldown cooldown;

    public FunTranslationsClient(
            @Qualifier("funTranslationsWebClient") WebClient webClient,
            FunTranslationsProperties properties,
            RateLimitCooldown cooldown) {
        this.webClient = webClient;
        this.shakespearePath = properties.shakespearePath();
        this.yodaPath = properties.yodaPath();
        this.cooldown = cooldown;
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
        if (cooldown.isRateLimited()) {
            log.warn("POST - /translated{} - Rate limited, skipping call", path);
            return Optional.empty();
        }

        FunTranslationResponse response = webClient.post()
                .uri(uriBuilder -> uriBuilder.path(path).build())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new FunTranslationRequest(text))
                .retrieve()
                .onStatus(status -> status.value() == 429,
                        clientResponse -> clientResponse.bodyToMono(FunTranslationErrorResponse.class)
                                .doOnNext(err -> {
                                    long seconds = err != null && err.retryAfter() != null
                                            ? err.retryAfter().longValue()
                                            : 60L;
                                    cooldown.setRateLimitedFor(seconds);
                                    log.warn("POST - /translated{} - 429 received, cooling down {}s", path, seconds);
                                })
                                .then(clientResponse.createError()))
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
    @SuppressWarnings("unused")
    private Optional<String> emptyFallback(String text, Throwable t) {
        log.warn("POST - /translated - FALLBACK");
        return Optional.empty();
    }
}
