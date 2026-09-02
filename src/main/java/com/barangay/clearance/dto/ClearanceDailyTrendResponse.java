package com.barangay.clearance.dto;

import java.time.LocalDate;

/**
 * Daily issuance volume point for trend charts.
 */
public record ClearanceDailyTrendResponse(
        LocalDate date,
        String label,
        int dayOfMonth,
        long totalIssued
) {
}
