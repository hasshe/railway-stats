package com.hs.railway_stats.service;

public interface RateLimiterService {

    boolean tryConsume(String ip);

    long getRemainingBlockSeconds(String ip);
}

