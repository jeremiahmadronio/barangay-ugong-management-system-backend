package com.barangay.cases.blotter.dto;

/**
 * Response payload containing archive and soft-delete summary statistics for Blotter records.
 *
 * @param totalArchived                 all-time total of archived blotter cases
 * @param totalArchivedThisMonth        blotter cases archived within the current calendar month
 * @param totalRecentlyDeleted          all-time active soft-deleted blotter cases in trash
 * @param totalRecentlyDeletedThisMonth blotter cases soft-deleted within the current calendar month
 */
public record BlotterLifecycleKpiResponse(
        long totalArchived,
        long totalArchivedThisMonth,
        long totalRecentlyDeleted,
        long totalRecentlyDeletedThisMonth
) {}