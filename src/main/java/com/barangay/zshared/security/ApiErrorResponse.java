package com.barangay.zshared.security;

/**
 * The error response returned when a request fails security checks.
 * Sent back as JSON with a code, message, and timestamp.
 */
public record ApiErrorResponse(

        // Short error label. Example: UNAUTHORIZED, ACCESS_DENIED
        String code,

        // A simple message explaining what went wrong.
        String message,

        // The exact time the error happened.
        String timestamp
) {}