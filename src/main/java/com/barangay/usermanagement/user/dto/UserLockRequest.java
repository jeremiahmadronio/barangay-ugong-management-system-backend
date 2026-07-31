package com.barangay.usermanagement.user.dto;

/**
 * DTO for locking or unlocking a user account.
 */
public record UserLockRequest(
        Boolean isLocked,
        Integer lockDurationMinutes,
        String reason
) {
}
