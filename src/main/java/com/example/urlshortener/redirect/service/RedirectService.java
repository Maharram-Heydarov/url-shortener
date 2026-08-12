package com.example.urlshortener.redirect.service;

import com.example.urlshortener.analytics.service.AnalyticsService;
import com.example.urlshortener.common.exception.ShortUrlDisabledException;
import com.example.urlshortener.common.exception.ShortUrlExpiredException;
import com.example.urlshortener.common.exception.ShortUrlNotFoundException;
import com.example.urlshortener.redirect.cache.CachedShortUrl;
import com.example.urlshortener.redirect.cache.ShortUrlCacheService;
import com.example.urlshortener.shorturl.entity.ShortUrl;
import com.example.urlshortener.shorturl.entity.ShortUrlStatus;
import com.example.urlshortener.shorturl.repository.ShortUrlRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedirectService {

    private final ShortUrlRepository shortUrlRepository;
    private final ShortUrlCacheService shortUrlCacheService;
    private final AnalyticsService analyticsService;

    public URI resolveTargetUrl(String shortCode) {

        return shortUrlCacheService
                .findByShortCode(shortCode)
                .map(this::resolveCachedTargetUrl)
                .orElseGet(() -> resolveDatabaseTargetUrl(shortCode));
    }

    private URI resolveDatabaseTargetUrl(String shortCode) {
        ShortUrl shortUrl = shortUrlRepository
                .findByShortCode(shortCode)
                .orElseThrow(
                        () -> new ShortUrlNotFoundException(shortCode)
                );

        validateAvailability(shortUrl);

        shortUrlCacheService.put(CachedShortUrl.from(shortUrl));
        recordClickBestEffort(
                shortUrl.getId(),
                shortUrl.getShortCode()
        );

        return URI.create(shortUrl.getOriginalUrl());
    }

    private URI resolveCachedTargetUrl(CachedShortUrl shortUrl) {
        validateAvailability(shortUrl);
        recordClickBestEffort(
                shortUrl.id(),
                shortUrl.shortCode()
        );

        return URI.create(shortUrl.originalUrl());
    }

    private void recordClickBestEffort(
            Long shortUrlId,
            String shortCode
    ) {
        try {
            analyticsService.recordClick(shortUrlId);
        } catch (RuntimeException exception) {
            log.warn(
                    "Unable to record redirect analytics for short URL {}",
                    shortCode,
                    exception
            );
        }
    }

    private void validateAvailability(ShortUrl shortUrl) {

        if (shortUrl.getStatus() == ShortUrlStatus.DISABLED) {
            throw new ShortUrlDisabledException(
                    shortUrl.getShortCode()
            );
        }

        LocalDateTime expiresAt = shortUrl.getExpiresAt();

        if (expiresAt != null
                && !expiresAt.isAfter(LocalDateTime.now())) {

            throw new ShortUrlExpiredException(
                    shortUrl.getShortCode()
            );
        }
    }

    private void validateAvailability(CachedShortUrl shortUrl) {

        if (shortUrl.status() == ShortUrlStatus.DISABLED) {
            throw new ShortUrlDisabledException(
                    shortUrl.shortCode()
            );
        }

        LocalDateTime expiresAt = shortUrl.expiresAt();

        if (expiresAt != null
                && !expiresAt.isAfter(LocalDateTime.now())) {

            throw new ShortUrlExpiredException(
                    shortUrl.shortCode()
            );
        }
    }
}
