package com.example.urlshortener.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for registering a new user")
public record RegisterRequest(

        @Schema(
                description = "Unique user email address",
                example = "user@example.com",
                maxLength = 320
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 320, message = "Email cannot exceed 320 characters")
        String email,

        @Schema(
                description = "Plain text password. It is hashed with BCrypt before storage.",
                example = "Secret123!",
                minLength = 8,
                maxLength = 72
        )
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password
) {
}
