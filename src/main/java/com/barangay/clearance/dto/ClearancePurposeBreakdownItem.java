package com.barangay.clearance.dto;

/**
 * Single item in Purpose distribution breakdown.
 */
public record ClearancePurposeBreakdownItem(
        String purpose,
        long count,
        double percentage
) {
}
