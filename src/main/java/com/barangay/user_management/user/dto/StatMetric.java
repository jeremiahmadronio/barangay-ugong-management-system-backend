package com.barangay.user_management.user.dto;

/**
 * Metric breakdown containing count, percentage change, trend direction, and formatted text.
 */
public record StatMetric(
        long count,
        double percentageChange,
        String trendDirection, // "UP", "DOWN", "NEUTRAL"
        String trendText        // e.g. "+15.5% vs last month"
) {
}
