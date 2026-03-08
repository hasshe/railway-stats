package com.hs.railway_stats.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hs.railway_stats.repository.entity.TripInfoMetric;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class MetricsCacheProvider {

    @Value("${metrics.cache.max-size:100}")
    private int maxSize;

    @Bean
    @Qualifier("metricsCache")
    public Cache<String, List<TripInfoMetric>> metricsCache() {
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .build();
    }
}

