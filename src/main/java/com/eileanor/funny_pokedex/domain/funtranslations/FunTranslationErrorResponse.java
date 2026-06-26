package com.eileanor.funny_pokedex.domain.funtranslations;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FunTranslationErrorResponse(
        TranslationError error,
        @JsonProperty("retry_after") Integer retryAfter) {
}
