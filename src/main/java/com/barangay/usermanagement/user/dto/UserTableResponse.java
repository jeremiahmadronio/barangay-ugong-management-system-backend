package com.barangay.usermanagement.user.dto;

import com.barangay.zshared.enumerated.UserStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload DTO for displaying User records in the User Management table.
 */
public record UserTableResponse(
        Long id,
        String photoBase64,
        String fullName,
        String firstName,
        String lastName,
        String username,
        String systemEmail,
        String roleName,
        String accountType,
        List<String> departmentAccess,
        UserStatus status,
        Boolean isLocked,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {
}
