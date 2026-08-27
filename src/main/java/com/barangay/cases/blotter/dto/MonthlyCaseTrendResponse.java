package com.barangay.cases.blotter.dto;
/**
 * Data transfer object representing a single monthly trend data point for charts and reports.
 *
 * @param label formatted month-year display label (e.g., "AUG 2026")
 * @param year the year of the reporting point
 * @param month the month value from 1 to 12
 * @param totalCases the total number of cases recorded in this month (including zero-filled counts)
 */
public record MonthlyCaseTrendResponse(
        String label,
        int year,
        int month,
        long totalCases
) {
}
