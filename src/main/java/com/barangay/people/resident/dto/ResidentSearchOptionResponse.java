package com.barangay.people.resident.dto;

public record ResidentSearchOptionResponse(
        Long id,
        Long personId,
        String fullName,
        String firstName,
        String lastName,
        String middleName,
        String barangayIdNumber,
        String email,
        String contactNumber,
        String completeAddress,
        Integer age,
        String gender,
        String civilStatus,
        String status,
        String photoBase64
) {}
