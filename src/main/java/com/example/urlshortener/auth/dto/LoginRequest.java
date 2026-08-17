package com.example.urlshortener.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body for JWT login")
public record LoginRequest(

        @Schema(
                description = "Registered user email address",
                example = "user@example.com",
                maxLength = 320
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 320, message = "Email cannot exceed 320 characters")
        String email,

        @Schema(
                description = "User password",
                example = "Secret123!",
                maxLength = 72
        )
        @NotBlank(message = "Password is required")
        @Size(max = 72, message = "Password cannot exceed 72 characters")
        String password
) {
}
