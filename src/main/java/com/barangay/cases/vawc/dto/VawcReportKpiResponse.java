package com.barangay.cases.vawc.dto;

/**
 * KPI response model for VAWC Department Reports.
 * Features the 4 primary metrics requested:
 * 1. Total VAWC Cases
 * 2. Total Referred Cases
 * 3. Most Agency Success Referred (Top Partner Agency)
 * 4. Total Dismissed Cases
 */
public record VawcReportKpiResponse(
        long totalCases,
        long totalReferredCases,
        String mostReferredAgency,
        long mostReferredAgencyCount,
        double mostReferredAgencySuccessRate,
        long totalDismissedCases,
        String totalCasesTrend,
        String totalReferredTrend,
        String totalDismissedTrend
) {}
