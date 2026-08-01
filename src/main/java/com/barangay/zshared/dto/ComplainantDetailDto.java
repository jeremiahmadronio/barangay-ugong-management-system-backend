package com.barangay.zshared.dto;
/**
 * Read-model for a {@code Complainant}, including the pre-built
 * {@code fullName} for display purposes.
 */
public record ComplainantDetailDto(
        Long id,
        String lastName,
        String firstName,
        String middleName,
        String fullName,
        String contactNumber,
        Short age,
        String gender,
        String civilStatus,
        String email,
        String completeAddress
) {}