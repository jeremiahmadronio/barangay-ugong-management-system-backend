package com.barangay.zshared.dto;

/**
 * Request payload for adding or updating a witness to a case.
 */
public record WitnessCreateDto(
        Long id,
        Long personId,
        String firstName,
        String lastName,
        String contactNumber,
        String completeAddress,
        String testimonyText,
        String testimonyFileName,
        byte[] testimonyFile
) {
    public WitnessCreateDto(
            String firstName,
            String lastName,
            String contactNumber,
            String completeAddress,
            String testimonyText,
            String testimonyFileName,
            byte[] testimonyFile
    ) {
        this(null, null, firstName, lastName, contactNumber, completeAddress, testimonyText, testimonyFileName, testimonyFile);
    }
}