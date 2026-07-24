package com.barangay.user_management.user.dto;

import com.barangay.zshared.enumerated.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for updating a user's status (ACTIVE / INACTIVE).
 */
public record UserStatusUpdateRequest(
        @NotNull(message = "Status is required")
        UserStatus status,
        String remarks
) {
}
