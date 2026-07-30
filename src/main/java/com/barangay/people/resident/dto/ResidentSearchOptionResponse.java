package com.barangay.people.resident.dto;

public record ResidentSearchOptionResponse(
        Long id,
        Long personId,
        String fullName,
        String barangayIdNumber,
        String email,
        String contactNumber,
        String status,
        String photoBase64
) {}
