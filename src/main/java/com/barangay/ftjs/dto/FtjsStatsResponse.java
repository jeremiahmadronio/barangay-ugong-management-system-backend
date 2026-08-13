package com.barangay.ftjs.dto;

public record FtjsStatsResponse(
        long totalCertificatesIssued,
        long totalCertificatedThisMonth,
        double issuedThisMonthTrendPercentage,
        String issuedThisMonthTrendHint,
        long originalIssuances,
        long reIssuances
) {
}
