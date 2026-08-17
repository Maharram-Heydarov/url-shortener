package com.example.urlshortener.redirect.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class ShortUrlCacheService {

    private static final String KEY_PREFIX = "short-url:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration defaultTtl;

    public ShortUrlCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.cache.short-url.default-ttl}") Duration defaultTtl
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.defaultTtl = defaultTtl;
    }

    public Optional<CachedShortUrl> findByShortCode(String shortCode) {
        String key = key(shortCode);

        try {
            String value = redisTemplate
                    .opsForValue()
                    .get(key);

            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(
                    objectMapper.readValue(value, CachedShortUrl.class)
            );

        } catch (JacksonException exception) {
            log.warn("Invalid cached short URL value for key {}", key);
            evict(shortCode);
            return Optional.empty();

        } catch (RuntimeException exception) {
            log.warn("Unable to read short URL cache for key {}", key, exception);
            return Optional.empty();
        }
    }

    public void put(CachedShortUrl cachedShortUrl) {
        Duration ttl = ttlFor(cachedShortUrl.expiresAt());

        if (ttl.compareTo(Duration.ZERO) <= 0) {
            return;
        }

        try {
            redisTemplate
                    .opsForValue()
                    .set(
                            key(cachedShortUrl.shortCode()),
                            objectMapper.writeValueAsString(cachedShortUrl),
                            ttl
                    );

        } catch (JacksonException exception) {
            log.warn(
                    "Unable to serialize short URL cache value for {}",
                    cachedShortUrl.shortCode(),
                    exception
            );

        } catch (RuntimeException exception) {
            log.warn(
                    "Unable to write short URL cache for {}",
                    cachedShortUrl.shortCode(),
                    exception
            );
        }
    }

    public void evict(String shortCode) {
        try {
            redisTemplate.delete(key(shortCode));
        } catch (RuntimeException exception) {
            log.warn("Unable to evict short URL cache for {}", shortCode, exception);
        }
    }

    private Duration ttlFor(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return defaultTtl;
        }

        Duration untilExpiration = Duration.between(
                LocalDateTime.now(),
                expiresAt
        );

        if (untilExpiration.compareTo(defaultTtl) < 0) {
            return untilExpiration;
        }

        return defaultTtl;
    }

    private String key(String shortCode) {
        return KEY_PREFIX + shortCode;
    }
}
