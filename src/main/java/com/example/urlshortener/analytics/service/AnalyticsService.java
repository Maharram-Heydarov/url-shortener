package com.example.urlshortener.analytics.service;

import com.example.urlshortener.analytics.dto.ShortUrlStatsResponse;
import com.example.urlshortener.auth.security.CurrentUserService;
import com.example.urlshortener.common.exception.ShortUrlAccessDeniedException;
import com.example.urlshortener.common.exception.ShortUrlNotFoundException;
import com.example.urlshortener.shorturl.entity.ShortUrl;
import com.example.urlshortener.shorturl.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ShortUrlRepository shortUrlRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public void recordClick(Long shortUrlId) {
        shortUrlRepository.incrementClickStats(
                shortUrlId,
                LocalDateTime.now()
        );
    }

    public ShortUrlStatsResponse getStats(Long id) {
        Long currentUserId = currentUserService.currentUserId();
        ShortUrl shortUrl = findOwnedShortUrl(id, currentUserId);

        return new ShortUrlStatsResponse(
                shortUrl.getId(),
                shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getClickCount(),
                shortUrl.getCreatedAt(),
                shortUrl.getLastAccessedAt(),
                shortUrl.getExpiresAt(),
                shortUrl.getStatus()
        );
    }

    private ShortUrl findOwnedShortUrl(
            Long id,
            Long userId
    ) {
        return shortUrlRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> {
                    if (shortUrlRepository.existsById(id)) {
                        return new ShortUrlAccessDeniedException(id);
                    }

                    return new ShortUrlNotFoundException(id);
                });
    }
}
