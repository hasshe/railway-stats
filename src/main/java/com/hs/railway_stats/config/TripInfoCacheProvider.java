package com.hs.railway_stats.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hs.railway_stats.dto.TripInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
public class TripInfoCacheProvider {
    @Value("${tripinfo.cache.expiry.hours:24}")
    private int expiryHours;

    @Value("${tripinfo.cache.max-size:100}")
    private int maxSize;

    @Bean
    public Cache<String, List<TripInfoResponse>> tripInfoCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(expiryHours, TimeUnit.HOURS)
                .maximumSize(maxSize)
                .build();
    }
}
