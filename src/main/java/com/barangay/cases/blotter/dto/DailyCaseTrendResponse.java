package com.barangay.cases.blotter.dto;

import java.time.LocalDate;
/**
 * Data transfer object representing a single daily trend data point for charts and reports.
 *
 * @param date the specific calendar date of the trend entry
 * @param label formatted date display label (e.g., "Aug 27")
 * @param dayOfMonth the numeric day of the month (1 to 31)
 * @param totalCases the total number of cases recorded on this date (including zero-filled counts)
 */
public record DailyCaseTrendResponse (
        LocalDate date,
        String label,
        int dayOfMonth,
        long totalCases
) {
}
