package com.barangay.zshared.security;

public record ApiErrorResponse(
        String code,
        String message,
        String timestamp
) {}