package com.barangay.people.person.dto;

/**
 * DTO payload for Person / Resident search results in User Creation modal.
 */
public record PersonOptionResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String contactNumber,
        String completeAddress
) {
}
