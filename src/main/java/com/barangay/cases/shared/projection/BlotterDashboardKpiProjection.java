package com.barangay.cases.shared.projection;

/**
 * Projection for Blotter Case KPI metrics (New cases, Pending, and Unresolved).
 */
public interface BlotterDashboardKpiProjection {
    Number getNewCasesThisWeek();
    Number getPendingCases();
    Number getUnresolvedCases();
}