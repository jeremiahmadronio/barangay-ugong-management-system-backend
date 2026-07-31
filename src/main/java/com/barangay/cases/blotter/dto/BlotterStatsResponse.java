package com.barangay.cases.blotter.dto;

/**
 * Summary statistics response DTO record for Blotter Dashboard.
 */
public record BlotterStatsResponse(
        long totalCases,
        long pendingCases,
        long inProgressCases,
        long resolvedCases,
        long forRecordCount,
        long formalComplaintCount
) {
}
