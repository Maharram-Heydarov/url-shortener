package com.example.urlshortener.shorturl.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "Request body for updating editable short URL fields")
public record UpdateShortUrlRequest(

        @Schema(
                description = "New destination URL. Only http and https URLs with a syntactically valid host are allowed.",
                example = "https://example.org/updated",
                maxLength = 2048
        )
        @Size(max = 2048, message = "URL cannot exceed 2048 characters")
        String originalUrl,

        @Schema(
                description = "New expiration date and time. Must be in the future.",
                example = "2026-10-01T12:00:00"
        )
        @Future(message = "Expiration date must be in the future")
        LocalDateTime expiresAt
) {
}
