package com.barangay.zshared.dto;

/**
 * Request payload for uploading or updating a piece of case evidence.
 */
public record EvidenceCreateDto (
        Long id,
        String evidenceTypeName,
        String fileName,
        String description,
        byte[] files
) {
    public EvidenceCreateDto(
            String evidenceTypeName,
            String fileName,
            String description,
            byte[] files
    ) {
        this(null, evidenceTypeName, fileName, description, files);
    }
}
