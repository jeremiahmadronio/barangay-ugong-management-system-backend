package com.barangay.cases.blotter.dto;

/**
 * Summary statistics response DTO record for Blotter Dashboard.
 * Includes dynamic month-over-month trend calculations from backend database.
 */
public record BlotterStatsResponse(
        long totalCases,
        long pendingCases,
        long inProgressCases,
        long resolvedCases,

        // Formal Complaint Dedicated KPI Stats
        long totalFormalComplaints,
        long pendingFormalComplaints,
        long mediationFormalComplaints,
        long settledFormalComplaints,

        // For the Record Dedicated KPI Stats
        long totalForTheRecord,
        long incidentOnlyForTheRecord,
        long consultationForTheRecord,
        long recentForTheRecord,

        // Dynamic Month-over-Month Trend Labels computed from Backend DB
        String totalCasesTrend,
        String totalForTheRecordTrend,
        String underMediationTrend,
        String settledCasesTrend
) {
}
