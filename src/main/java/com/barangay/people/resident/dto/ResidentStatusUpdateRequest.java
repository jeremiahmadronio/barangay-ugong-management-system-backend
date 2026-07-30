package com.barangay.people.resident.dto;

import com.barangay.zshared.enumerated.ResidentStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload DTO for updating a Resident's status (ACTIVE, INACTIVE, DECEASED, MOVED_OUT).
 */
public record ResidentStatusUpdateRequest(
        @NotNull(message = "Status is required")
        ResidentStatus status,
        String remarks
) {
}
