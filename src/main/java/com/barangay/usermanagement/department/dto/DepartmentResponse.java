package com.barangay.usermanagement.department.dto;

/**
 * Lightweight DTO payload for Department dropdown selection.
 */
public record DepartmentResponse(
        Long id,
        String name
) {
}
