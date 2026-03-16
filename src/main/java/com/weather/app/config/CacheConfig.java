package com.weather.app.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Dati meteo: 10 minuti — Open-Meteo aggiorna ogni ~15 min
     */
    public static final String WEATHER_CACHE = "weather";

    /**
     * Geocoding/autocomplete: 24 ore — le città non cambiano
     */
    public static final String GEOCODING_CACHE = "geocoding";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                caffeineCache(WEATHER_CACHE, Duration.ofMinutes(10), 100),
                caffeineCache(GEOCODING_CACHE, Duration.ofHours(24), 500)
        ));
        return manager;
    }

    private CaffeineCache caffeineCache(String name, Duration ttl, long maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maxSize)
                .recordStats()
                .build());
    }
}
