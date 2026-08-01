package com.barangay.zshared.dto;

/**
 * Read-model for a {@code Witness}, including their pre-built full name
 * and any uploaded testimony file.
 */
public record WitnessDetailDto(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String contactNumber,
        String completeAddress,
        String testimonyText,
        byte[] testimonyFile
) {}