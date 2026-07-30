package com.barangay.people.resident.dto;

public record ResidentFileRequest(
        String fileName,
        String fileType,
        String fileBase64
) {}
