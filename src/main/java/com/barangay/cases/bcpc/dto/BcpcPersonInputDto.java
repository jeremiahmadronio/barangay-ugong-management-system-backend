package com.barangay.cases.bcpc.dto;

import java.time.LocalDate;

/**
 * Universal input DTO for persons involved in BCPC cases (Reporter, Child, Guardian, Witnesses).
 * Accommodates both selected resident records (via residentId / personId) and manual entries.
 */
public record BcpcPersonInputDto(
        Long residentId,
        Long personId,
        String firstName,
        String middleName,
        String lastName,
        String suffix,
        String contactNumber,
        String completeAddress,
        Short age,
        String gender,
        LocalDate birthDate,
        String civilStatus,
        String email
) {
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) sb.append(firstName.trim()).append(" ");
        if (middleName != null && !middleName.isBlank()) sb.append(middleName.trim()).append(" ");
        if (lastName != null && !lastName.isBlank()) sb.append(lastName.trim());
        if (suffix != null && !suffix.isBlank()) sb.append(" ").append(suffix.trim());
        return sb.toString().trim();
    }
}

