package com.barangay.clearance.dto;

/**
 * Response DTO for Clearance Archive & Trash Vault KPI metrics.
 */
public record ClearanceLifecycleKpiResponse(
        long totalArchived,
        long totalArchivedThisMonth,
        long totalRecentlyDeleted,
        long totalRecentlyDeletedThisMonth
) {
}
