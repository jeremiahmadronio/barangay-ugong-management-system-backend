package com.barangay.cases.blotter.dto;

public record BlotterStatsResponse(
        long totalCases,
        String totalCasesTrend,
        long totalForTheRecord,
        String totalForTheRecordTrend,
        long inProgressCases,
        long resolvedCases,
        String resolvedCasesTrend
) {
}
