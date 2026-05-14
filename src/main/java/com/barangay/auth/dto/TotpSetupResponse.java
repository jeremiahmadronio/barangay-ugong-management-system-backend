package com.barangay.auth.dto;
/**
 * Returned after Step 1 of TOTP setup.
 * The frontend should display the QR code as an image for the user to scan.
 */
public record TotpSetupResponse(
        // Temporary secret — must be passed back in Step 2
        String secret,

        // Base64-encoded QR code image — use as <img src="..."> in the frontend
        String qrCode
) {}
