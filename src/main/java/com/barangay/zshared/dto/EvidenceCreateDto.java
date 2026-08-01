package com.barangay.zshared.dto;
/**
 * Request payload for uploading a piece of case evidence.
 *
 * @param evidenceTypeName  the evidence type name, matched against {@code EvidenceType}
 *              (e.g. "Photo", "Medical Certificate")
 * @param files raw binary content of the uploaded file
 */
public record EvidenceCreateDto (

        String evidenceTypeName,
        byte[] files
) {
}

