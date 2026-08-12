package com.example.urlshortener.redirect.cache;

import com.example.urlshortener.shorturl.entity.ShortUrl;
import com.example.urlshortener.shorturl.entity.ShortUrlStatus;

import java.time.LocalDateTime;

public record CachedShortUrl(
        Long id,
        String shortCode,
        String originalUrl,
        ShortUrlStatus status,
        LocalDateTime expiresAt
) {

    public static CachedShortUrl from(ShortUrl shortUrl) {
        return new CachedShortUrl(
                shortUrl.getId(),
                shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getStatus(),
                shortUrl.getExpiresAt()
        );
    }
}
