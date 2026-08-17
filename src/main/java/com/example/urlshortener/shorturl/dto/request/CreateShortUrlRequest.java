package com.example.urlshortener.shorturl.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "Request body for creating a short URL")
public record CreateShortUrlRequest(

        @Schema(
                description = "Destination URL. Only http and https URLs with a syntactically valid host are allowed.",
                example = "https://example.com/products/123",
                maxLength = 2048
        )
        @NotBlank(message = "Original URL is required")
        @Size(max = 2048, message = "URL cannot exceed 2048 characters")
        String originalUrl,

        @Schema(
                description = "Optional custom short code. Allowed characters: letters, numbers, hyphen, and underscore.",
                example = "my-product",
                minLength = 3,
                maxLength = 32
        )
        @Size(min = 3, max = 32, message = "Custom alias must be between 3 and 32 characters")
        @Pattern(
                regexp = "^[a-zA-Z0-9_-]+$",
                message = "Custom alias can contain only letters, numbers, '-' and '_'"
        )
        String customAlias,

        @Schema(
                description = "Optional expiration date and time. Must be in the future. Expiration is calculated from this field, not stored as a separate status.",
                example = "2026-09-01T12:00:00"
        )
        @Future(message = "Expiration date must be in the future")
        LocalDateTime expiresAt
) {
}
