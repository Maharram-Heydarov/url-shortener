package com.example.urlshortener.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Standard API error response")
public record ErrorResponse(
        @Schema(description = "Stable application error code", example = "INVALID_URL")
        String code,

        @Schema(description = "Human-readable error message", example = "Invalid URL: ftp://example.com/file")
        String message,

        @Schema(description = "Error response timestamp", example = "2026-08-12T09:30:00")
        LocalDateTime timestamp
) {
}
