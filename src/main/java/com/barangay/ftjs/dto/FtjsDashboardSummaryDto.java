package com.barangay.ftjs.dto;

public record FtjsDashboardSummaryDto(
        long issuedToday,
        long issuedThisWeek,
        long femaleBeneficiaries,
        double femalePercentage,
        long youthEntrants,
        double youthPercentage,
        long totalActiveVolume
) {}