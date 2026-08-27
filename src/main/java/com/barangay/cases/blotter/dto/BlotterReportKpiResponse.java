package com.barangay.cases.blotter.dto;

import java.time.LocalDate;

/**
 * Response payload containing KPI statistics for Blotter Reports within a date range.
 */
public record BlotterReportKpiResponse(
        LocalDate startDate,
        LocalDate endDate,
        Long departmentId,
        long totalCases,
        long settledCases,
        double settlementRate,
        long activeMediations,
        long luponReferrals
) {
}
