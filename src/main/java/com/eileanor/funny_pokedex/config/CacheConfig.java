package com.eileanor.funny_pokedex.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    private static final List<String> CACHE_NAMES = List.of("pokemon", "pokemon-translated");

    // Local profile: Caffeine in-memory cache.
    // Zero external dependencies — runs without any infrastructure.
    @Bean
    @Profile("local")
    public CacheManager caffeineCacheManager(@Value("${app.cache.ttl:24h}") Duration ttl) {
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
            @Value("${app.cache.ttl:24h}") Duration ttl) {
        // GenericJacksonJsonRedisSerializer embeds @class metadata so the deserializer
        // can reconstruct the right type without needing Serializable.
        var serializer = GenericJacksonJsonRedisSerializer.builder().build();
        var config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .initialCacheNames(java.util.Set.copyOf(CACHE_NAMES))
                .build();
    }
}
