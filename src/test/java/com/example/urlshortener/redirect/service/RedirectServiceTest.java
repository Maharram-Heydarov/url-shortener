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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RedirectServiceTest {

    private ShortUrlRepository shortUrlRepository;
    private ShortUrlCacheService shortUrlCacheService;
    private AnalyticsService analyticsService;
    private RedirectService redirectService;

    @BeforeEach
    void setUp() {
        shortUrlRepository = mock(ShortUrlRepository.class);
        shortUrlCacheService = mock(ShortUrlCacheService.class);
        analyticsService = mock(AnalyticsService.class);

        redirectService = new RedirectService(
                shortUrlRepository,
                shortUrlCacheService,
                analyticsService
        );
    }

    @Test
    void resolvesCachedActiveShortUrlWithoutDatabaseLookup() {
        CachedShortUrl cachedShortUrl = new CachedShortUrl(
                1L,
                "abc1234",
                "https://example.com",
                ShortUrlStatus.ACTIVE,
                LocalDateTime.now().plusDays(1)
        );

        when(shortUrlCacheService.findByShortCode("abc1234"))
                .thenReturn(Optional.of(cachedShortUrl));

        URI targetUrl = redirectService.resolveTargetUrl("abc1234");

        assertEquals(URI.create("https://example.com"), targetUrl);
        verifyNoInteractions(shortUrlRepository);
        verify(analyticsService).recordClick(1L);
    }

    @Test
    void resolvesCachedActiveShortUrlWhenAnalyticsFails() {
        CachedShortUrl cachedShortUrl = new CachedShortUrl(
                1L,
                "abc1234",
                "https://example.com",
                ShortUrlStatus.ACTIVE,
                LocalDateTime.now().plusDays(1)
        );

        when(shortUrlCacheService.findByShortCode("abc1234"))
                .thenReturn(Optional.of(cachedShortUrl));
        doThrow(new RuntimeException("analytics unavailable"))
                .when(analyticsService)
                .recordClick(1L);

        URI targetUrl = redirectService.resolveTargetUrl("abc1234");

        assertEquals(URI.create("https://example.com"), targetUrl);
        verifyNoInteractions(shortUrlRepository);
        verify(analyticsService).recordClick(1L);
    }

    @Test
    void resolvesDatabaseShortUrlAndCachesItOnMiss() {
        ShortUrl shortUrl = activeShortUrl();

        when(shortUrlCacheService.findByShortCode("abc1234"))
                .thenReturn(Optional.empty());
        when(shortUrlRepository.findByShortCode("abc1234"))
                .thenReturn(Optional.of(shortUrl));

        URI targetUrl = redirectService.resolveTargetUrl("abc1234");

        assertEquals(URI.create("https://example.com"), targetUrl);
        verify(shortUrlCacheService).put(any(CachedShortUrl.class));
        verify(analyticsService).recordClick(1L);
    }

    @Test
    void resolvesDatabaseShortUrlWhenAnalyticsFails() {
        ShortUrl shortUrl = activeShortUrl();

        when(shortUrlCacheService.findByShortCode("abc1234"))
                .thenReturn(Optional.empty());
        when(shortUrlRepository.findByShortCode("abc1234"))
                .thenReturn(Optional.of(shortUrl));
        doThrow(new RuntimeException("analytics unavailable"))
                .when(analyticsService)
                .recordClick(1L);

        URI targetUrl = redirectService.resolveTargetUrl("abc1234");

        assertEquals(URI.create("https://example.com"), targetUrl);
        verify(shortUrlCacheService).put(any(CachedShortUrl.class));
        verify(analyticsService).recordClick(1L);
    }

    @Test
    void missingShortUrlStillThrowsNotFound() {
        when(shortUrlCacheService.findByShortCode("missing"))
                .thenReturn(Optional.empty());
        when(shortUrlRepository.findByShortCode("missing"))
                .thenReturn(Optional.empty());

        assertThrows(
                ShortUrlNotFoundException.class,
                () -> redirectService.resolveTargetUrl("missing")
        );
        verifyNoInteractions(analyticsService);
    }

    @Test
    void expiredShortUrlDoesNotRedirectOrRecordAnalytics() {
        ShortUrl shortUrl = activeShortUrl();
        shortUrl.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        when(shortUrlCacheService.findByShortCode("abc1234"))
                .thenReturn(Optional.empty());
        when(shortUrlRepository.findByShortCode("abc1234"))
                .thenReturn(Optional.of(shortUrl));

        assertThrows(
                ShortUrlExpiredException.class,
                () -> redirectService.resolveTargetUrl("abc1234")
        );
        verify(shortUrlCacheService, never()).put(any(CachedShortUrl.class));
        verifyNoInteractions(analyticsService);
    }

    @Test
    void disabledShortUrlDoesNotRedirectOrRecordAnalytics() {
        ShortUrl shortUrl = activeShortUrl();
        shortUrl.setStatus(ShortUrlStatus.DISABLED);

        when(shortUrlCacheService.findByShortCode("abc1234"))
                .thenReturn(Optional.empty());
        when(shortUrlRepository.findByShortCode("abc1234"))
                .thenReturn(Optional.of(shortUrl));

        assertThrows(
                ShortUrlDisabledException.class,
                () -> redirectService.resolveTargetUrl("abc1234")
        );
        verify(shortUrlCacheService, never()).put(any(CachedShortUrl.class));
        verifyNoInteractions(analyticsService);
    }

    private ShortUrl activeShortUrl() {
        return ShortUrl.builder()
                .id(1L)
                .shortCode("abc1234")
                .originalUrl("https://example.com")
                .status(ShortUrlStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(1))
                .clickCount(0L)
                .build();
    }
}
