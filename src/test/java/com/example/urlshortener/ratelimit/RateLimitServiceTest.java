package com.example.urlshortener.ratelimit;

import com.example.urlshortener.common.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitServiceTest {

    private StringRedisTemplate redisTemplate;
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        rateLimitService = new RateLimitService(
                redisTemplate,
                2,
                Duration.ofMinutes(1)
        );
    }

    @Test
    void firstRequestUsesAtomicLuaScriptWithTtl() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                eq(List.of("rate-limit:create-url:user:1")),
                eq("60000")
        )).thenReturn(1L);

        assertDoesNotThrow(() -> rateLimitService.checkCreateUrlLimit(1L));

        verify(redisTemplate).execute(
                any(DefaultRedisScript.class),
                eq(List.of("rate-limit:create-url:user:1")),
                eq("60000")
        );
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
        assertTrue(rateLimitService.incrementWithTtlScriptText().contains("INCR"));
        assertTrue(rateLimitService.incrementWithTtlScriptText().contains("PEXPIRE"));
    }

    @Test
    void requestsUnderLimitSucceed() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString()
        )).thenReturn(2L);

        assertDoesNotThrow(() -> rateLimitService.checkCreateUrlLimit(1L));
    }

    @Test
    void limitExceededThrowsRateLimitExceededException() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString()
        )).thenReturn(3L);

        assertThrows(
                RateLimitExceededException.class,
                () -> rateLimitService.checkCreateUrlLimit(1L)
        );
    }

    @Test
    void redisFailureAllowsRequest() {
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString()
        )).thenThrow(new RuntimeException("Redis unavailable"));

        assertDoesNotThrow(() -> rateLimitService.checkCreateUrlLimit(1L));
    }
}
