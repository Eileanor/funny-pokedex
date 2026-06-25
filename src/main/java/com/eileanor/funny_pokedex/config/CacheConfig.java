package com.eileanor.funny_pokedex.config;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import com.eileanor.funny_pokedex.domain.PokemonResponse;
import com.github.benmanes.caffeine.cache.Caffeine;

import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableCaching
public class CacheConfig {

    private final List<String> CACHE_NAMES;
    private Duration ttl;

    public CacheConfig(CacheProperties cacheProperties) {
        this.ttl = cacheProperties.ttl();
        this.CACHE_NAMES = cacheProperties.cacheNames();
    }

    // Local profile: Caffeine in-memory cache.
    // Zero external dependencies — runs without any infrastructure.
    @Bean
    @Profile("local")
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(CACHE_NAMES);
        manager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(ttl));
        return manager;
    }

    // Production profile: Redis shared cache — all replicas share one cache,
    // so upstream quota protection is cluster-wide, not per-instance.
    @Bean
    @Profile("prod")
    public CacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        var serializer = pokemonResponseSerializer(objectMapper);
        var config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .initialCacheNames(Set.copyOf(CACHE_NAMES))
                .build();
    }

    // Serialize PokemonResponse directly to/from JSON by type — avoids the @class /
    // PolymorphicTypeValidator round-trip that causes LinkedHashMap casts in
    // Jackson 3.x.
    static RedisSerializer<Object> pokemonResponseSerializer(ObjectMapper objectMapper) {
        return new RedisSerializer<Object>() {
            @Override
            public byte[] serialize(Object value) {
                return value == null ? new byte[0] : objectMapper.writeValueAsBytes(value);
            }

            @Override
            public Object deserialize(byte[] bytes) {
                return (bytes == null || bytes.length == 0) ? null
                        : objectMapper.readValue(bytes, PokemonResponse.class);
            }
        };
    }
}
