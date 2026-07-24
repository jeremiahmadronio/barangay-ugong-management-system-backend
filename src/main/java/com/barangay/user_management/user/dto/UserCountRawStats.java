package com.barangay.user_management.user.dto;

/**
 * Raw count statistics for current and previous month user metrics.
 */
public record UserCountRawStats(
        long currentTotal,
        long previousTotal,
        long currentActive,
        long previousActive,
        long currentLocked,
        long previousLocked,
        long currentInactive,
        long previousInactive
) {
}
