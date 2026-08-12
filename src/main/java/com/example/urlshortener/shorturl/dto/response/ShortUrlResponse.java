package com.example.urlshortener.shorturl.dto.response;

import com.example.urlshortener.shorturl.entity.ShortUrlStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Short URL data returned by management endpoints")
public record ShortUrlResponse(
        @Schema(description = "Database id of the short URL", example = "1")
        Long id,

        @Schema(description = "Short code or custom alias", example = "abc1234")
        String shortCode,

        @Schema(description = "Full public short URL", example = "http://localhost:8080/r/abc1234")
        String shortUrl,

        @Schema(description = "Original destination URL", example = "https://example.com/products/123")
        String originalUrl,

        @Schema(description = "Management status. Expiration is calculated from expiresAt.", example = "ACTIVE")
        ShortUrlStatus status,

        @Schema(description = "Creation date and time", example = "2026-08-12T09:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Optional expiration date and time", example = "2026-09-01T12:00:00")
        LocalDateTime expiresAt
) {
}
