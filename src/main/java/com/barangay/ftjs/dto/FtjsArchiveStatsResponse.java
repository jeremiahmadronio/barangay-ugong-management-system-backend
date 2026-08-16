package com.barangay.ftjs.dto;

/**
 * DTO response containing statistics for archived and deleted FTJS records.
 */
public record FtjsArchiveStatsResponse (
        Long totalArchived,
        Long totalArchivedThisMonth,
        String totalArchiveThisMonthTrend,
        Long totalArchiveOriginalIssuance,
        Long totalRecentlyDeleted
) {
}
