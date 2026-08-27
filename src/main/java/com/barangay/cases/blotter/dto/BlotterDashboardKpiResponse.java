package com.barangay.cases.blotter.dto;

import java.time.LocalDate;

/**
 * Response payload for the 4 core Blotter Dashboard KPIs.
 *
 * @param weekStartDate              start date of the current calendar week (Monday)
 * @param weekEndDate                end date of the current calendar week (Sunday)
 * @param totalNewCasesThisWeek      total root cases filed starting this Monday
 * @param totalPendingCases          total active endmost cases with PENDING status
 * @param upcomingMediationsThisWeek total scheduled mediation sessions this week
 * @param totalUnresolvedCases       total active blotter cases (Pending + Under Mediation, excluding Conciliation)
 */
public record BlotterDashboardKpiResponse(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        long totalNewCasesThisWeek,
        long totalPendingCases,
        long upcomingMediationsThisWeek,
        long totalUnresolvedCases
) {}