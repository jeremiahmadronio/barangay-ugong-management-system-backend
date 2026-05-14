package com.barangay.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the initial login step.
 */
public record LoginRequest(
        // User's registered system email
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        // User's current password
        @NotBlank(message = "Password is required")
        String password
) {}
