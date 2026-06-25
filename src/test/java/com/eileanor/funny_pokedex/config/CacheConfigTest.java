package com.eileanor.funny_pokedex.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.eileanor.funny_pokedex.domain.PokemonResponse;

import tools.jackson.databind.ObjectMapper;

class CacheConfigTest {

    private RedisSerializer<Object> serializer;

    @BeforeEach
    void setUp() {
        serializer = CacheConfig.pokemonResponseSerializer(new ObjectMapper());
    }

    @Test
    @DisplayName("pokemonResponseSerializer serializes and deserializes PokemonResponse correctly")
    void serialize_pokemonResponse_returnsJsonBytes() {
        PokemonResponse response = PokemonResponse.builder()
                .name("mewtwo")
                .description("A rare Pokémon.")
                .habitat("rare")
                .isLegendary(true)
                .build();

        byte[] bytes = serializer.serialize(response);

        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes)).contains("mewtwo");
    }

    @Test
    @DisplayName("serialize null returns empty byte array")
    void serialize_null_returnsEmptyByteArray() {
        assertThat(serializer.serialize(null)).isEmpty();
    }

    @Test
    @DisplayName("deserialize valid bytes returns PokemonResponse")
    void deserialize_validBytes_returnsPokemonResponse() {
        PokemonResponse original = PokemonResponse.builder()
                .name("pikachu")
                .description("Electric mouse Pokémon.")
                .habitat("forest")
                .isLegendary(false)
                .build();

        byte[] bytes = serializer.serialize(original);
        PokemonResponse result = (PokemonResponse) serializer.deserialize(bytes);

        assertThat(result).isEqualTo(original);
    }

    @Test
    @DisplayName("deserialize null returns null")
    void deserialize_null_returnsNull() {
        assertThat(serializer.deserialize(null)).isNull();
    }

    @Test
    @DisplayName("deserialize empty array returns null")
    void deserialize_emptyArray_returnsNull() {
        assertThat(serializer.deserialize(new byte[0])).isNull();
    }

    @Test
    @DisplayName("redisCacheManager configures TTL and cache names")
    void redisCacheManager_configuresTtlAndCacheNames() {
        var cacheProperties = new CacheProperties(Duration.ofMinutes(30), List.of("pokemon", "pokemon-translated"));
        var cacheConfig = new CacheConfig(cacheProperties);
        var connectionFactory = mock(RedisConnectionFactory.class);

        RedisCacheManager cacheManager = (RedisCacheManager) cacheConfig.redisCacheManager(connectionFactory,
                new ObjectMapper());
        cacheManager.afterPropertiesSet();

        assertThat(cacheManager.getCacheNames()).containsExactlyInAnyOrder("pokemon", "pokemon-translated");
    }
}
