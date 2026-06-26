package com.eileanor.funny_pokedex.config.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;

class PokemonCacheResolverTest {

    // Provides real Method instances by name without touching production code.
    interface StubMethods {
        void getTranslatedPokemon();
        void getPokemon();
    }

    private CacheManager cacheManager;
    private PokemonCacheResolver resolver;
    private Cache pokemonCache;
    private Cache translatedCache;

    @BeforeEach
    void setUp() {
        cacheManager = mock(CacheManager.class);
        CacheProperties props = new CacheProperties(
                Duration.ofHours(24),
                new CacheProperties.CacheName("pokemon", "pokemon-translated"));
        resolver = new PokemonCacheResolver(cacheManager, props);

        pokemonCache = mock(Cache.class);
        translatedCache = mock(Cache.class);
        when(cacheManager.getCache("pokemon")).thenReturn(pokemonCache);
        when(cacheManager.getCache("pokemon-translated")).thenReturn(translatedCache);
    }

    @Test
    @DisplayName("getCacheNames returns pokemon-translated for getTranslatedPokemon method")
    void getCacheNames_getTranslatedPokemon_returnsPokemonTranslatedCache() throws NoSuchMethodException {
        assertThat(resolveFor("getTranslatedPokemon")).containsExactly(translatedCache);
    }

    @Test
    @DisplayName("getCacheNames returns pokemon for any other method name")
    void getCacheNames_otherMethod_returnsPokemonCache() throws NoSuchMethodException {
        assertThat(resolveFor("getPokemon")).containsExactly(pokemonCache);
    }

    @SuppressWarnings("unchecked")
    private Collection<Cache> resolveFor(String methodName) throws NoSuchMethodException {
        Method method = StubMethods.class.getMethod(methodName);
        CacheOperationInvocationContext<?> context = mock(CacheOperationInvocationContext.class);
        when(context.getMethod()).thenReturn(method);
        return (Collection<Cache>) resolver.resolveCaches(context);
    }
}
