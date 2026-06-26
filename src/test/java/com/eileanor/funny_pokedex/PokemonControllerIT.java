package com.eileanor.funny_pokedex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.eileanor.funny_pokedex.client.FunTranslationsClient;
import com.eileanor.funny_pokedex.client.PokeApiClient;
import com.eileanor.funny_pokedex.config.client.FunTranslationsProperties;
import com.eileanor.funny_pokedex.config.rate_limit.RateLimitCooldown;
import com.eileanor.funny_pokedex.domain.PokemonResponse;
import com.eileanor.funny_pokedex.domain.pokeapi.FlavorTextEntry;
import com.eileanor.funny_pokedex.domain.pokeapi.NamedResource;
import com.eileanor.funny_pokedex.domain.pokeapi.PokemonSpeciesResponse;
import com.eileanor.funny_pokedex.error.PokemonNotFoundException;
import com.eileanor.funny_pokedex.service.PokemonService;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("prod")
@Testcontainers(parallel = false)
class PokemonControllerIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort())
            .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1))
            .withStartupTimeout(Duration.ofSeconds(60));

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.timeout", () -> "5s");
        registry.add("spring.data.redis.client-type", () -> "lettuce");
    }

    @MockitoBean
    PokeApiClient pokeApiClient;
    @MockitoBean
    FunTranslationsClient funTranslationsClient;

    @Autowired
    PokemonService pokemonService;
    @Autowired
    CacheManager cacheManager;
    @Autowired
    RedisConnectionFactory redisConnectionFactory;
    @Autowired
    FunTranslationsProperties properties;
    @Autowired
    RateLimitCooldown rateLimitCooldown;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private static final String POKEMON_NAME = "mewtwo";
    private static final String STANDARD_DESC = "It was created by a scientist after years of horrific gene splicing.";
    private static final String YODA_DESC = "Created by a scientist after years of horrific gene splicing, it was.";

    @BeforeEach
    void setUp() {
        reset(pokeApiClient, funTranslationsClient);
        try (var conn = redisConnectionFactory.getConnection()) {
            conn.serverCommands().flushAll();
        }
    }

    @Test
    @DisplayName("test connectivity to Redis cache")
    void testRedisConnectivity() {
        // Given
        String cacheName = "pokemon";
        String key = POKEMON_NAME;
        PokemonResponse value = PokemonResponse.builder()
                .name(POKEMON_NAME)
                .description(STANDARD_DESC)
                .isLegendary(true)
                .build();

        // When
        cacheManager.getCache(cacheName).putIfAbsent(key, value);
        PokemonResponse cachedValue = cacheManager.getCache(cacheName).get(key, PokemonResponse.class);

        // Then
        assertThat(cachedValue).isNotNull();
        assertThat(cachedValue.name()).isEqualTo(POKEMON_NAME);
        assertThat(cachedValue.description()).isEqualTo(STANDARD_DESC);
        assertThat(cachedValue.isLegendary()).isTrue();
    }

    @Test
    @DisplayName("second request for same pokemon is served from Redis cache")
    void getPokemon_returnsCorrectResponse() {
        when(pokeApiClient.fetchSpecies(POKEMON_NAME))
                .thenReturn(mewtwoSpecies());

        // when — two calls to the service
        PokemonResponse firstCall = pokemonService.getPokemon(POKEMON_NAME);
        PokemonResponse secondCall = pokemonService.getPokemon(POKEMON_NAME);

        // then — PokeApiClient was only invoked once
        // the second call was served entirely from Redis
        verify(pokeApiClient, times(1))
                .fetchSpecies(POKEMON_NAME);

        // and both responses are identical
        assertThat(firstCall).isEqualTo(secondCall);
        assertThat(firstCall.name()).isEqualTo(POKEMON_NAME);
        assertThat(firstCall.description()).isEqualTo(STANDARD_DESC);
        assertThat(firstCall.isLegendary()).isTrue();
    }

    @Test
    @DisplayName("translated endpoint caches separately from basic endpoint")
    void translatedAndBasicUseIsolatedCacheNamespaces() {
        // given
        when(pokeApiClient.fetchSpecies(POKEMON_NAME))
                .thenReturn(mewtwoSpecies());

        // mewtwo is legendary → Yoda translation
        when(funTranslationsClient.translateYoda(
                anyString()))
                .thenReturn(Optional.of(YODA_DESC));

        // when
        PokemonResponse basic = pokemonService.getPokemon(POKEMON_NAME);
        PokemonResponse translated = pokemonService.getTranslatedPokemon(POKEMON_NAME);

        // then — PokeAPI was called twice because the two cache namespaces
        // (cache:basic:mewtwo and cache:translated:mewtwo) are independent
        verify(pokeApiClient, times(2))
                .fetchSpecies(POKEMON_NAME);

        // descriptions differ
        assertThat(basic.description()).isEqualTo(STANDARD_DESC);
        assertThat(translated.description()).isEqualTo(YODA_DESC);
    }

    @Test
    @DisplayName("translated endpoint is cached — FunTranslations called only once")
    void translatedResponseIsCached() {
        // given
        when(pokeApiClient.fetchSpecies(POKEMON_NAME))
                .thenReturn(mewtwoSpecies());

        when(funTranslationsClient.translateYoda(
                anyString()))
                .thenReturn(Optional.of(YODA_DESC));

        // when — first call (cache miss)
        pokemonService.getTranslatedPokemon(POKEMON_NAME);

        // assert the entry is physically in Redis before the second call;
        // if the write was lost this fails clearly rather than as a Mockito count
        // mismatch
        assertThat(cacheManager.getCache("pokemon").get(POKEMON_NAME))
                .isNull();
        assertThat(cacheManager.getCache("pokemon-translated").get(POKEMON_NAME))
                .isNotNull();

        // reset interaction counts so the second-call verify is isolated
        clearInvocations(funTranslationsClient, pokeApiClient);

        // when — second call (should be a cache hit)
        pokemonService.getTranslatedPokemon(POKEMON_NAME);

        // then — no upstream calls; the second call was served from Redis
        verifyNoInteractions(funTranslationsClient, pokeApiClient);
    }

    @Test
    @DisplayName("fallback description is cached when translation fails")
    void fallbackDescriptionIsCachedOnTranslationFailure() {
        // given — translation fails (rate limit, network error, etc.)
        when(pokeApiClient.fetchSpecies(POKEMON_NAME))
                .thenReturn(mewtwoSpecies());

        when(funTranslationsClient.translateYoda(anyString()))
                .thenReturn(Optional.empty()); // simulates 429/timeout → circuit breaker emptyFallback

        // when
        PokemonResponse firstCall = pokemonService.getTranslatedPokemon(POKEMON_NAME);
        PokemonResponse secondCall = pokemonService.getTranslatedPokemon(POKEMON_NAME);

        // then — fallback (standard) description is returned
        assertThat(firstCall.description()).isEqualTo(STANDARD_DESC);

        // and the fallback result was cached — FunTranslations not called again
        verify(funTranslationsClient, times(1))
                .translateYoda(anyString());

        // both calls return the same result
        assertThat(firstCall).isEqualTo(secondCall);
    }

    @Test
    @DisplayName("unknown pokemon propagates 404 and is not cached")
    void unknownPokemonIsNotCached() {
        // given
        when(pokeApiClient.fetchSpecies("unknown"))
                .thenThrow(new PokemonNotFoundException("unknown"));

        // when / then — exception is thrown
        assertThatThrownBy(() -> pokemonService.getPokemon("unknown"))
                .isInstanceOf(PokemonNotFoundException.class);

        // and nothing was cached — a second call still hits the client
        assertThatThrownBy(() -> pokemonService.getPokemon("unknown"))
                .isInstanceOf(PokemonNotFoundException.class);

        verify(pokeApiClient, times(2))
                .fetchSpecies("unknown");
    }

    @Test
    @DisplayName("cache entry is physically present in Redis after first request")
    void cacheEntryStoredInRedis() {
        // given
        when(pokeApiClient.fetchSpecies(POKEMON_NAME))
                .thenReturn(mewtwoSpecies());

        // when
        pokemonService.getPokemon(POKEMON_NAME);

        // then — the entry physically exists in the Redis-backed cache
        assertThat(cacheManager.getCache("pokemon").get(POKEMON_NAME, PokemonResponse.class))
                .isNotNull()
                .extracting(PokemonResponse::name)
                .isEqualTo(POKEMON_NAME);
    }

    @Test
    @DisplayName("no interactions with upstream clients after cache is warm")
    void noUpstreamCallsAfterCacheIsWarm() {
        // given — warm the cache
        when(pokeApiClient.fetchSpecies(POKEMON_NAME))
                .thenReturn(mewtwoSpecies());

        pokemonService.getPokemon(POKEMON_NAME); // cache miss — populates Redis

        // reset interaction counts after warm-up
        clearInvocations(pokeApiClient, funTranslationsClient);

        // when — N subsequent requests
        pokemonService.getPokemon(POKEMON_NAME);
        pokemonService.getPokemon(POKEMON_NAME);
        pokemonService.getPokemon(POKEMON_NAME);
        pokemonService.getPokemon(POKEMON_NAME);
        pokemonService.getPokemon(POKEMON_NAME);

        // then — zero upstream calls despite 5 requests
        verifyNoInteractions(pokeApiClient, funTranslationsClient);
    }

    @Test
    @DisplayName("RedisRateLimitCooldown is not rate-limited by default")
    void rateLimitCooldown_notRateLimitedByDefault() {
        assertThat(rateLimitCooldown.isRateLimited()).isFalse();
    }

    @Test
    @DisplayName("RedisRateLimitCooldown reports rate-limited after setRateLimitedFor")
    void rateLimitCooldown_isRateLimitedAfterSet() {
        rateLimitCooldown.setRateLimitedFor(60);
        assertThat(rateLimitCooldown.isRateLimited()).isTrue();
    }

    @Test
    @DisplayName("RedisRateLimitCooldown stores the key with a TTL in Redis")
    void rateLimitCooldown_keyHasTtlInRedis() {
        rateLimitCooldown.setRateLimitedFor(30);
        Long ttl = stringRedisTemplate.getExpire("funtranslations:rate-limited");
        assertThat(ttl).isPositive();
    }

    private PokemonSpeciesResponse mewtwoSpecies() {
        return PokemonSpeciesResponse.builder()
                .name(POKEMON_NAME)
                .isLegendary(true)
                .habitat(new NamedResource("rare"))
                .flavorTextEntries(List.of(
                        new FlavorTextEntry(
                                STANDARD_DESC,
                                new NamedResource("en"))))
                .build();
    }

}
