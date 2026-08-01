package com.barangay.zshared.dto;
/**
 * Read-model for a stored {@code CaseEvidence} record, including its raw file bytes.
 */
public record EvidenceDetailDto(
        Long id,
        String evidenceTypeName,
        byte[] files
){}