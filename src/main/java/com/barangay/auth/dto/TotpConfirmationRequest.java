package com.barangay.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for Step 2 of TOTP setup.
 */
public record TotpConfirmationRequest(
        // The secret returned from Step 1
        @NotBlank(message = "Secret is required")
        String secret,

        // The 6-digit code shown in the authenticator app
        @NotBlank(message = "Verification code is required")
        @Size(min = 6, max = 6, message = "TOTP code must be exactly 6 digits")
        String code
) {}
