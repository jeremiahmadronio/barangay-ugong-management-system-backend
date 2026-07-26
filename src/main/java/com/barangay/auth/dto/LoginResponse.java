package com.barangay.auth.dto;

import java.util.Set;

/**
 * Response returned after each step of the login flow.
 * Status values: MFA_REQUIRED, CHANGE_PASSWORD_REQUIRED, SUCCESS
 */
public record LoginResponse(
        // Current state of the login flow
        String status,

        // ID of the authenticated user
        Long userId,

        // Role name of the user (e.g. ROOT_ADMIN, STAFF)
        String role,

        // Department names the user belongs to — null if not yet fully authenticated
        Set<String> departments,

        // JWT token — only present when status is SUCCESS
        String token,

        // Whether the user has TOTP (authenticator app) enabled
        boolean totpEnabled,

        // Whether the user has a backup email registered
        boolean hasBackupEmail,

        // User profile details
        String firstName,
        String lastName,
        String username,
        String photoUrl
) {}