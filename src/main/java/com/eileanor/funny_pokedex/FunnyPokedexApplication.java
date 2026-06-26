package com.eileanor.funny_pokedex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.eileanor.funny_pokedex.config.rate_limit.RateLimitProperties;
import com.eileanor.funny_pokedex.config.cache.CacheProperties;
import com.eileanor.funny_pokedex.config.client.FunTranslationsProperties;
import com.eileanor.funny_pokedex.config.client.PokeApiProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        CacheProperties.class,
        FunTranslationsProperties.class,
        PokeApiProperties.class,
        RateLimitProperties.class
})
public class FunnyPokedexApplication {

    public static void main(String[] args) {
        SpringApplication.run(FunnyPokedexApplication.class, args);
    }

}
