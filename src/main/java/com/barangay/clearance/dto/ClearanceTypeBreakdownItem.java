package com.barangay.clearance.dto;

/**
 * Single item in Certificate Type breakdown.
 */
public record ClearanceTypeBreakdownItem(
        String certTitle,
        long count,
        double percentage
) {
}
