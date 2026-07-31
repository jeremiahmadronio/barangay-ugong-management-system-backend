package com.barangay.usermanagement.permission.dto;

/**
 * Lightweight DTO payload for Permission selection lists and checkboxes.
 */
public record PermissionResponse(
        Long id,
        String permissionName,
        String module
) {
}
