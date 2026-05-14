package com.barangay.auth.dto;
/**
 * Returned after TOTP has been successfully enabled for the user.
 */
public record TotpEnableSuccessResponse(
        // Confirmation message — always "TOTP_ENABLED_SUCCESSFULLY"
        String status
) {}
