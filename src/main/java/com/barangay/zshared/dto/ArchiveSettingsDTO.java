package com.barangay.zshared.dto;

public record ArchiveSettingsDTO(
        Boolean autoDeleteEnabled,
        Integer deleteAfterDays
) {}
