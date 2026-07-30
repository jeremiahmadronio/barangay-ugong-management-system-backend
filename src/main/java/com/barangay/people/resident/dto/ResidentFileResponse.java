package com.barangay.people.resident.dto;

public record ResidentFileResponse(
        Long id,
        String fileName,
        String fileType,
        String fileBase64
) {}
