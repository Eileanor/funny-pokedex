package com.eileanor.funny_pokedex.config.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class WebClientConfig {

    private String pokeApiBaseUrl;
    private String funTranslationsBaseUrl;

    public WebClientConfig(PokeApiProperties pokeApiProperties, FunTranslationsProperties funTranslationsProperties) {
        this.pokeApiBaseUrl = pokeApiProperties.baseUrl();
        this.funTranslationsBaseUrl = funTranslationsProperties.baseUrl();
    }

    @Bean("pokeApiWebClient")
    public WebClient pokeApiWebClient() {
        return WebClient.builder()
                .baseUrl(pokeApiBaseUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    @Bean("funTranslationsWebClient")
    public WebClient funTranslationsWebClient() {
        return WebClient.builder()
                .baseUrl(funTranslationsBaseUrl)
                .build();
    }
}
