package com.barangay.clearance.dto;

import java.time.LocalDate;

/**
 * KPI statistics response for Clearance Reports.
 */
public record ClearanceReportKpiResponse(
        LocalDate startDate,
        LocalDate endDate,
        Long templateId,
        long totalIssued,
        String mostIssuedCertName,
        long mostIssuedCertCount,
        long totalExpired,
        long maleCount,
        long femaleCount,
        double malePercentage,
        double femalePercentage
) {
}
