package com.barangay.user_management.user.dto;

/**
 * DTO for locking or unlocking a user account.
 */
public record UserLockRequest(
        Boolean isLocked,
        Integer lockDurationMinutes,
        String reason
) {
}
