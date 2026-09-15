package com.interviewcoach.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void fallsBackToLocalLimitWhenRedisIsUnavailable() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString()))
                .thenThrow(new DataAccessResourceFailureException("redis unavailable"));
        RateLimiter limiter = new RateLimiter(redisTemplate);

        assertTrue(limiter.allow("test:user:1", 2, 60));
        assertTrue(limiter.allow("test:user:1", 2, 60));
        assertFalse(limiter.allow("test:user:1", 2, 60));
    }

    @Test
    void rejectsInvalidLimitConfiguration() {
        RateLimiter limiter = new RateLimiter(redisTemplate);

        assertFalse(limiter.allow("", 1, 60));
        assertFalse(limiter.allow("key", 0, 60));
        assertFalse(limiter.allow("key", 1, 0));
    }
}
