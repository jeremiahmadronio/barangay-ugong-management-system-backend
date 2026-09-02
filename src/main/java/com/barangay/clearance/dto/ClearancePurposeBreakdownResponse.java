package com.barangay.clearance.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Response for Purpose distribution breakdown.
 */
public record ClearancePurposeBreakdownResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalVolume,
        String highestPurpose,
        long highestCount,
        List<ClearancePurposeBreakdownItem> breakdown
) {
}
