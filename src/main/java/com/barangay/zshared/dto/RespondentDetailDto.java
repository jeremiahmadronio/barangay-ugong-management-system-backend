package com.barangay.zshared.dto;

/**
 * Read-model for a {@code Respondent}, including the pre-built
 * {@code fullName} and relationship info relative to the complainant.
 */
public record RespondentDetailDto(
        Long id,
        String lastName,
        String firstName,
        String middleName,
        String fullName,
        String contactNumber,
        Short age,
        String gender,
        String completeAddress,
        String relationshipToComplainant,
        Boolean currentlyLivingWithComplainant
) {}