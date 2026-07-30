package com.barangay.people.resident.dto;

import com.barangay.zshared.enumerated.ResidentStatus;

/**
 * Filter criteria DTO for Resident Management table queries.
 */
public record ResidentFilterRequest(
        String search,
        String status,
        String gender,
        String ageGroup
) {
}
