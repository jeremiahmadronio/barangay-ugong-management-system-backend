package com.barangay.auth.dto;

import com.barangay.zshared.enumerated.MfaType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for the MFA verification step.
 * Supports EMAIL OTP, BACKUP EMAIL OTP, and TOTP (authenticator app).
 */
public record MfaRequest(
        // User's registered system email
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        // The verification code entered by the user
        @NotBlank(message = "Verification code is required")
        @Size(min = 6, max = 8, message = "Invalid verification code length")
        String code,

        // The MFA method used — EMAIL, BACKUP, or TOTP
        @NotNull(message = "MFA type is required")
        MfaType type
) {}