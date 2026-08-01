package com.barangay.zshared.dto;
/**
 * Request payload for adding a witness to a case.
 */
public record WitnessCreateDto(
        String firstName,
        String lastName,
        String contactNumber,
        String completeAddress,
        String testimonyText,
        byte[] testimonyFile
) {}