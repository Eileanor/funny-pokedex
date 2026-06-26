package com.eileanor.funny_pokedex.config.cache;

import java.util.Collection;
import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.AbstractCacheResolver;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.stereotype.Component;

@Component("pokemonCacheResolver")
public class PokemonCacheResolver extends AbstractCacheResolver {

    private final CacheProperties cacheProperties;

    public PokemonCacheResolver(CacheManager cacheManager, CacheProperties cacheProperties) {
        super(cacheManager);
        this.cacheProperties = cacheProperties;
    }

    @Override
    protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
        return "getTranslatedPokemon".equals(context.getMethod().getName())
                ? List.of(cacheProperties.name().pokemonTranslated())
                : List.of(cacheProperties.name().pokemon());
    }
}
