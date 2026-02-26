package com.hs.railway_stats.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter.
 * Allows at most {@code app.rate-limiter.max-requests} within a
 * {@code app.rate-limiter.window-seconds} sliding window.
 * IPs that exceed the limit are blocked for {@code app.rate-limiter.timeout-seconds}.
 */
@Service
public class RateLimiterServiceImpl implements RateLimiterService {

    private final int maxRequests;
    private final long windowSeconds;
    private final long timeoutSeconds;

    private final Map<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();
    private final Map<String, Instant> blockedUntil = new ConcurrentHashMap<>();

    public RateLimiterServiceImpl(
            @Value("${app.rate-limiter.max-requests:20}") int maxRequests,
            @Value("${app.rate-limiter.window-seconds:300}") long windowSeconds,
            @Value("${app.rate-limiter.timeout-seconds:900}") long timeoutSeconds) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public synchronized boolean tryConsume(String ip) {
        Instant now = Instant.now();

        Instant blockedExpiry = blockedUntil.get(ip);
        if (blockedExpiry != null) {
            if (now.isBefore(blockedExpiry)) {
                return false;
            }
            blockedUntil.remove(ip);
            requestLog.remove(ip);
        }

        Deque<Instant> timestamps = requestLog.computeIfAbsent(ip, k -> new ArrayDeque<>());

        Instant windowStart = now.minusSeconds(windowSeconds);
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
            timestamps.pollFirst();
        }

        timestamps.addLast(now);

        if (timestamps.size() > maxRequests) {
            blockedUntil.put(ip, now.plusSeconds(timeoutSeconds));
            requestLog.remove(ip);
            return false;
        }

        return true;
    }

    @Override
    public long getRemainingBlockSeconds(String ip) {
        Instant expiry = blockedUntil.get(ip);
        if (expiry == null) return 0;
        long remaining = Instant.now().until(expiry, ChronoUnit.SECONDS);
        return Math.max(0, remaining);
    }
}

