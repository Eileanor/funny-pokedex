package com.eileanor.funny_pokedex.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("pokeApiWebClient")
    public WebClient pokeApiWebClient(@Value("${app.pokeapi.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();
    }

    @Bean("funTranslationsWebClient")
    public WebClient funTranslationsWebClient(@Value("${app.funtranslations.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
