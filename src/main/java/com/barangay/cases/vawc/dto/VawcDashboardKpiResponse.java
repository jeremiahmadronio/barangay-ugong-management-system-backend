package com.barangay.cases.vawc.dto;

import java.time.LocalDate;

/**
 * Response payload for the 4 core VAWC Dashboard KPIs.
 * Computes weekly metrics starting Monday to Sunday:
 * 1. Total VAWC Cases filed this week
 * 2. Total active BPOs this week
 * 3. Total referred cases this week
 * 4. Total withdrawn cases this week
 *
 * @param weekStartDate               start date of current calendar week (Monday)
 * @param weekEndDate                 end date of current calendar week (Sunday)
 * @param totalCasesThisWeek          total cases filed this week (Monday to Sunday)
 * @param totalCasesTrend             trend vs previous week percentage
 * @param totalActiveBpoThisWeek      total active BPO (issued/served) this week
 * @param totalReferredCasesThisWeek  total cases referred to partner agencies this week
 * @param totalWithdrawnCasesThisWeek total withdrawn cases this week
 */
public record VawcDashboardKpiResponse(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        long totalCasesThisWeek,
        String totalCasesTrend,
        long totalActiveBpoThisWeek,
        long totalReferredCasesThisWeek,
        long totalWithdrawnCasesThisWeek
) {}
