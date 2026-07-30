package com.barangay.people.resident.dto;

public record ResidentRelativeResponse(
        Long id,
        String fullName,
        String address,
        Integer age,
        String gender,
        String civilStatus,
        String contactNumber,
        String householdNumber,
        String barangayIdNumber,
        String status,
        String relationshipType,
        String photoBase64
) {}
