package com.barangay.cases.bcpc.dto;

import java.time.LocalDate;

/**
 * Operational KPI Metrics for BCPC Weekly Dashboard:
 * 1. New Intakes This Week (Monday to Sunday) + Trend vs Previous Week
 * 2. Needs Assessment / Action (Active cases pending initial assessment / action)
 * 3. Active Intervention Monitoring (Children under active diversion / intervention)
 * 4. Pending Agency Feedback (Referrals awaiting partner agency reports)
 */
public record BcpcDashboardKpiResponse(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        long totalNewCasesThisWeek,
        String newCasesTrend,
        long totalNeedsAssessment,
        long totalActiveInterventions,
        long totalPendingAgencyFeedback
) {}

