package com.barangay.zshared.dto;

/**
 * Read-model for a stored {@code CaseEvidence} record, including its type, file name, description, and raw file bytes.
 */
public record EvidenceDetailDto(
        Long id,
        String evidenceTypeName,
        String fileName,
        String description,
        byte[] files
){}