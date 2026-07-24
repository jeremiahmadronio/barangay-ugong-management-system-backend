package com.barangay.user_management.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Data Transfer Object for creating a new Officer user account.
 * Note: Role is automatically assigned as "OFFICER" in the backend.
 */
public record UserCreateRequest(

        @NotNull(message = "Person ID is required.")
        Long personId,

        @NotBlank(message = "System email is required.")
        @Email(message = "Please provide a valid email address.")
        @Size(max = 100, message = "Email cannot exceed 100 characters.")
        String systemEmail,

        @Email(message = "Please provide a valid backup email address.")
        @Size(max = 100, message = "Backup email cannot exceed 100 characters.")
        String systemBackupEmail,

        @NotEmpty(message = "At least one department must be assigned.")
        Set<Long> departmentIds,

        Set<Long> permissionIds,

        Boolean isActivated
) {
}
