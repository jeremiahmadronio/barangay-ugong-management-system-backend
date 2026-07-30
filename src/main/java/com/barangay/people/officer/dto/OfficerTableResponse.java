package com.barangay.people.officer.dto;

public record OfficerTableResponse(
        Long id,
        Long personId,
        String name,
        String email,
        Long departmentId,
        String departmentName,
        String position,
        int activeCases,
        String status,
        Boolean isGlobal,
        String photoBase64
) {}
