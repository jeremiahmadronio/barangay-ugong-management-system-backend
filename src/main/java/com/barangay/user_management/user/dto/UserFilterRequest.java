package com.barangay.user_management.user.dto;

import com.barangay.zshared.enumerated.UserStatus;

/**
 * Filter criteria DTO for User Management table queries.
 * Note: Role serves as the Account Type.
 */
public record UserFilterRequest(
        String search,
        String roleName,
        String departmentName,
        UserStatus status
) {
}
