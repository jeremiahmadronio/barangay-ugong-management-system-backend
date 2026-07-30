package com.barangay.zshared.dto;

public record ArchiveStatsDTO(
        long totalArchived,
        long totalArchivedResidents,
        long totalArchivedOfficers,
        long totalArchivedUsers
) {}
