package com.barangay.zshared.dto;
/**
 * Request payload for creating a new {@code Respondent} record in a case.
 */
public record RespondentCreateDto(
        String lastName,
        String firstName,
        String middleName,
        String contactNumber,
        Short age,
        String gender,
        String completeAddress,
        String relationshipToComplainant,
        Boolean currentlyLivingWithComplainant
) {}