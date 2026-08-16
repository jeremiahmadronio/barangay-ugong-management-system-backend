package com.barangay.ftjs.dto;

/**
 * DTO response for FTJS analytics report Key Performance Indicators (KPIs).
 */
public record FtjsReportKpiResponse(
        long totalRecords,
        String totalRecordsSubtitle,
        long firstTimeIssuances,
        double firstTimeIssuanceShare,
        String firstTimeIssuanceSubtitle,
        String mostCommonPurpose,
        double mostCommonPurposeShare,
        String mostCommonPurposeSubtitle,
        String mostCommonAgeRange,
        double mostCommonAgeRangeShare,
        String mostCommonAgeRangeSubtitle
) {}
