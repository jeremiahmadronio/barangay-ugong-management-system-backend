package com.barangay.cases.bcpc.dto;

import java.time.LocalDate;

/**
 * Response payload containing KPI statistics for BCPC Reports within a date range.
 */
public record BcpcReportKpiResponse(
        LocalDate startDate,
        LocalDate endDate,
        long totalCases,
        long settledCases,
        double settlementRate,
        long activeInterventions,
        long agencyReferrals
) {
}

