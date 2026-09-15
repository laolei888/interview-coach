package com.interviewcoach.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

/**
 * 简易 Redis 限流器 — 滑动窗口计数
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, LocalWindow> localWindows = new ConcurrentHashMap<>();
    private final Clock clock = Clock.systemUTC();

    /**
     * 判断是否允许请求
     * @param key 限流标识（如 "rate:ai:用户ID"）
     * @param maxRequests 窗口内最大请求数
     * @param windowSeconds 时间窗口（秒）
     */
    public boolean allow(String key, int maxRequests, int windowSeconds) {
        if (key == null || key.isBlank() || maxRequests < 1 || windowSeconds < 1) {
            return false;
        }
        String redisKey = "rate:" + key;
        try {
            Long current = redisTemplate.opsForValue().increment(redisKey);
            if (current != null && current == 1) {
                redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
            }
            return current != null && current <= maxRequests;
        } catch (DataAccessException e) {
            log.warn("Redis 限流不可用，已降级为本机限流: {}", e.getMessage());
            return allowLocally(redisKey, maxRequests, windowSeconds);
        }
    }

    private boolean allowLocally(String key, int maxRequests, int windowSeconds) {
        long now = clock.millis();
        long expiresAt = now + TimeUnit.SECONDS.toMillis(windowSeconds);
        LocalWindow window = localWindows.compute(key, (ignored, existing) -> {
            if (existing == null || existing.expiresAt <= now) {
                return new LocalWindow(expiresAt);
            }
            existing.count.incrementAndGet();
            return existing;
        });
        if ((now & 63) == 0) {
            localWindows.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        }
        return window.count.get() <= maxRequests;
    }

    private static final class LocalWindow {
        private final AtomicInteger count = new AtomicInteger(1);
        private final long expiresAt;

        private LocalWindow(long expiresAt) {
            this.expiresAt = expiresAt;
        }
    }
}
