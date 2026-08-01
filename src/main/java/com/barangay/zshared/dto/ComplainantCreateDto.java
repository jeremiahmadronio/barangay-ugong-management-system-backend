package com.barangay.zshared.dto;
/**
 * Request payload for creating a new {@code Complainant} record.
 * Wraps the basic personal information of the complaining party.
 */
public record ComplainantCreateDto(
        String lastName,
        String firstName,
        String middleName,
        String contactNumber,
        Short age,
        String gender,
        String civilStatus,
        String email,
        String completeAddress
) {}