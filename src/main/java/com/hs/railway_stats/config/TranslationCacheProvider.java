package com.hs.railway_stats.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hs.railway_stats.dto.TranslationDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TranslationCacheProvider {

    @Value("${translation.cache.max-size:500}")
    private int maxSize;

    @Bean
    @Qualifier("translationCache")
    public Cache<String, TranslationDto> translationCache() {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .build();
    }
}
