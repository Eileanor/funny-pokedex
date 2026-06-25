package com.eileanor.funny_pokedex.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.funtranslations")
public record FunTranslationsProperties(String baseUrl, String shakespearePath, String yodaPath) {
}
