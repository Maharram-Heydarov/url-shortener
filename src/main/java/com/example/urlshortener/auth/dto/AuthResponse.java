package com.example.urlshortener.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response containing a JWT access token")
public record AuthResponse(
        @Schema(
                description = "JWT access token used in the Swagger Authorize dialog or Authorization header",
                example = "eyJhbGciOiJIUzI1NiJ9..."
        )
        String accessToken,

        @Schema(
                description = "Token type. Use this as Bearer in HTTP Authorization headers.",
                example = "Bearer"
        )
        String tokenType,

        @Schema(
                description = "Authenticated user id",
                example = "1"
        )
        Long userId,

        @Schema(
                description = "Authenticated user email",
                example = "user@example.com"
        )
        String email
) {
}
