package com.barangay.clearance.dto;

/**
 * Monthly issuance volume point for trend charts.
 */
public record ClearanceMonthlyTrendResponse(
        String monthLabel,
        int year,
        int month,
        long totalIssued
) {
}
