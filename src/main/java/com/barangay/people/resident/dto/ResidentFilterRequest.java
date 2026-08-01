package com.barangay.people.resident.dto;

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
