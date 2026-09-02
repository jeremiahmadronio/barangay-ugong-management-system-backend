package com.barangay.clearance.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Response for Certificate Type distribution breakdown.
 */
public record ClearanceTypeBreakdownResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalVolume,
        List<ClearanceTypeBreakdownItem> breakdown
) {
}
