package com.barangay.zshared.dto;

/**
 * Request payload for uploading a piece of case evidence.
 */
public record EvidenceCreateDto (
        String evidenceTypeName,
        String fileName,
        String description,
        byte[] files
) {}
