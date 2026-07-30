package com.barangay.people.resident.dto;

import com.barangay.zshared.enumerated.ResidentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response payload DTO for displaying Resident records in the Resident Management table.
 */
public record ResidentTableResponse(
        Long id,
        String barangayIdNumber,
        String photoBase64,
        String fullName,
        String firstName,
        String lastName,
        String middleName,
        String contactNumber,
        String email,
        String completeAddress,
        String householdNumber,
        Boolean isHeadOfFamily,
        ResidentStatus status,
        Integer age,
        String gender,
        String civilStatus,
        LocalDate dateOfResidency,
        LocalDateTime createdAt
) {
}
