package com.barangay.cases.vawc.dto;

public record VawcStatsResponse(
        long totalCases,
        String totalCasesTrend,
        long totalReferredCases,
        long totalActiveBpo,
        long totalWithdrawnCases
) {
}
