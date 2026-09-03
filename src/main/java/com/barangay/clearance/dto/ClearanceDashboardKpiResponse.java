package com.barangay.clearance.dto;

import java.time.LocalDate;

/**
 * Response payload for the 4 core Clearance Operational Dashboard KPIs:
 * 1. Total Issued Today
 * 2. Total Issued This Week (Monday to Sunday)
 * 3. Total Active Clearances
 * 4. Most Issued Certificate (This Week)
 */
public record ClearanceDashboardKpiResponse(
        LocalDate today,
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        long totalIssuedToday,
        long totalIssuedThisWeek,
        long totalActiveClearances,
        String mostIssuedCertThisWeek,
        long mostIssuedCertCountThisWeek
) {}
