package com.barbu.api.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be 3–50 characters")
        @Schema(description = "3–50 characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 120, message = "Password must be at least 8 characters")
        @Schema(description = "8–120 characters")
        String password
) {}