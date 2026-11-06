package com.barangay.cases.bcpc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for BCPC Case KPI Statistics.
 *
 * @param totalCases total active BCPC cases
 * @param totalCasesTrend month-over-month trend comparison formatted (e.g., "+100.0% vs last month")
 * @param totalActiveCases pending or ongoing cases under mediation/intervention
 * @param totalReferredCases cases referred to outside agencies (e.g. LSWDO, WCPD)
 * @param totalSettledCases successfully settled/resolved cases
 * @param totalOngoing alias for totalActiveCases for frontend compatibility
 */
public record BcpcStatsResponse(
        long totalCases,
        String totalCasesTrend,
        long totalActiveCases,
        long totalReferredCases,
        long totalSettledCases,
        @JsonProperty("totalOngoing")
        long totalOngoing
) {
    public BcpcStatsResponse(
            long totalCases,
            String totalCasesTrend,
            long totalActiveCases,
            long totalReferredCases,
            long totalSettledCases
    ) {
        this(totalCases, totalCasesTrend, totalActiveCases, totalReferredCases, totalSettledCases, totalActiveCases);
    }
}

