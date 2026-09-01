package com.barangay.cases.lupon.dto;

import java.time.LocalDate;

/**
 * Response payload for the 4 core Lupong Tagapamayapa Dashboard KPIs.
 *
 * @param weekStartDate             start date of the current calendar week (Monday)
 * @param weekEndDate               end date of the current calendar week (Sunday)
 * @param totalReferredThisWeek     total distinct cases referred to Lupon this week
 * @param totalUnderConciliation    total active cases currently under conciliation
 * @param upcomingHearingsThisWeek  total scheduled conciliation/hearing sessions this week
 * @param totalCfaIssuedThisWeek    total Certificate to File Action (CFA) issued this week
 */
public record LuponDashboardKpiResponse(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        long totalReferredThisWeek,
        long totalUnderConciliation,
        long upcomingHearingsThisWeek,
        long totalCfaIssuedThisWeek
) {}
