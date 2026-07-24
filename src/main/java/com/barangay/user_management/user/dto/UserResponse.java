package com.barangay.user_management.user.dto;

import com.barangay.zshared.enumerated.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Data Transfer Object for User response payload.
 */
public record UserResponse(
        Long id,
        String username,
        String systemEmail,
        UserStatus status,
        Boolean isNewAccount,
        Long personId,
        String personFullName,
        Long roleId,
        String roleName,
        Set<String> departments,
        Set<String> customPermissions,
        LocalDateTime createdAt
) {
}
