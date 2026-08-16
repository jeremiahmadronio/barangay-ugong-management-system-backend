package com.barangay.ftjs.dto;

/**
 * DTO response containing FTJS statistical counters and monthly trends.
 */
public record FtjsStatsResponse(
        long totalCertificatesIssued,
        long totalCertificatedThisMonth,
        double issuedThisMonthTrendPercentage,
        String issuedThisMonthTrendHint,
        long originalIssuances,
        long reIssuances
) {
}
