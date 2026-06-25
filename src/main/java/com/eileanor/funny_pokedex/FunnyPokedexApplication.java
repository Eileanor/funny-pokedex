package com.eileanor.funny_pokedex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.eileanor.funny_pokedex.config.CacheProperties;
import com.eileanor.funny_pokedex.config.FunTranslationsProperties;
import com.eileanor.funny_pokedex.config.PokeApiProperties;
import com.eileanor.funny_pokedex.config.RateLimitProperties;

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
