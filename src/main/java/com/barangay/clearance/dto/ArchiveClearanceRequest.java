package com.barangay.clearance.dto;

import jakarta.validation.constraints.Size;

/**
 * Request payload for archiving an issued certificate.
 */
public record ArchiveClearanceRequest(
        @Size(max = 250, message = "Archive remarks cannot exceed 250 characters")
        String remarks
) {
}
