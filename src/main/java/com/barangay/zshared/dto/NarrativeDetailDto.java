package com.barangay.zshared.dto;
/**
 * Read-model for a stored {@code NarrativeStatement}, including its raw file bytes.
 */
public record NarrativeDetailDto(
        Long id,
        String fileName,
        String statementSummary,
        byte[] fileData,
        boolean hasFile

) {}