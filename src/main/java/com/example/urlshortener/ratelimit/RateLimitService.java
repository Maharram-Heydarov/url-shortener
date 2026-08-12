package com.example.urlshortener.ratelimit;

import com.example.urlshortener.common.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class RateLimitService {

    private static final String CREATE_URL_KEY_PREFIX =
            "rate-limit:create-url:user:";
    private static final String INCREMENT_WITH_TTL_SCRIPT = """
            local current = redis.call('INCR', KEYS[1])

            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end

            return current
            """;

    private final DefaultRedisScript<Long> incrementWithTtlScript =
            new DefaultRedisScript<>(
                    INCREMENT_WITH_TTL_SCRIPT,
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;
    private final long createUrlLimit;
    private final Duration createUrlWindow;

    public RateLimitService(
            StringRedisTemplate redisTemplate,
            @Value("${app.rate-limit.create-url.limit}") long createUrlLimit,
            @Value("${app.rate-limit.create-url.window}") Duration createUrlWindow
    ) {
        this.redisTemplate = redisTemplate;
        this.createUrlLimit = createUrlLimit;
        this.createUrlWindow = createUrlWindow;
    }

    public void checkCreateUrlLimit(Long userId) {
        String key = CREATE_URL_KEY_PREFIX + userId;

        try {
            Long currentCount = redisTemplate.execute(
                    incrementWithTtlScript,
                    List.of(key),
                    String.valueOf(createUrlWindow.toMillis())
            );

            if (currentCount == null) {
                return;
            }

            if (currentCount > createUrlLimit) {
                throw new RateLimitExceededException();
            }

        } catch (RateLimitExceededException exception) {
            throw exception;

        } catch (RuntimeException exception) {
            log.warn("Unable to apply create URL rate limit for user {}", userId, exception);
        }
    }

    String incrementWithTtlScriptText() {
        return INCREMENT_WITH_TTL_SCRIPT;
    }
}
