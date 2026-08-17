package com.example.urlshortener.analytics.dto;

import com.example.urlshortener.shorturl.entity.ShortUrlStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Basic analytics for one short URL")
public record ShortUrlStatsResponse(
        @Schema(description = "Database id of the short URL", example = "1")
        Long id,

        @Schema(description = "Short code or custom alias", example = "abc1234")
        String shortCode,

        @Schema(description = "Original destination URL", example = "https://example.com")
        String originalUrl,

        @Schema(description = "Total successful redirects recorded for this short URL", example = "150")
        Long clickCount,

        @Schema(description = "Creation date and time", example = "2026-08-12T09:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Most recent successful redirect date and time", example = "2026-08-12T10:15:00")
        LocalDateTime lastAccessedAt,

        @Schema(description = "Optional expiration date and time", example = "2026-09-01T12:00:00")
        LocalDateTime expiresAt,

        @Schema(description = "Management status. Expiration is calculated from expiresAt.", example = "ACTIVE")
        ShortUrlStatus status
) {
}
