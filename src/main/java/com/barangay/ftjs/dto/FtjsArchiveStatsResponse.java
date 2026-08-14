package com.barangay.ftjs.dto;

public record FtjsArchiveStatsResponse (
        Long totalArchived,
        Long totalArchivedThisMonth,
        String totalArchiveThisMonthTrend,
        Long totalArchiveOriginalIssuance,
        Long totalRecentlyDeleted
) {
}
