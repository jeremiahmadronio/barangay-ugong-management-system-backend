package com.barangay.zshared.audit.dto;

public record AuditLogStatsResponse(
        long totalEntries,
        long todayEntries,
        long warningCount,
        long criticalCount
) {
}
