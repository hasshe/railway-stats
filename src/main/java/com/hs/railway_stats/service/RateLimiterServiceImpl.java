package com.hs.railway_stats.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter.
 * Allows at most MAX_REQUESTS within a WINDOW_SECONDS sliding window.
 * IPs that exceed the limit are blocked for TIMEOUT_SECONDS.
 */
@Service
public class RateLimiterServiceImpl implements RateLimiterService {

    private static final int MAX_REQUESTS = 20;
    private static final long WINDOW_SECONDS = 5 * 60L;   // 5 minutes
    private static final long TIMEOUT_SECONDS = 15 * 60L; // 15 minutes

    private final Map<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();
    private final Map<String, Instant> blockedUntil = new ConcurrentHashMap<>();

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

        Instant windowStart = now.minusSeconds(WINDOW_SECONDS);
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
            timestamps.pollFirst();
        }

        timestamps.addLast(now);

        if (timestamps.size() > MAX_REQUESTS) {
            blockedUntil.put(ip, now.plusSeconds(TIMEOUT_SECONDS));
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

