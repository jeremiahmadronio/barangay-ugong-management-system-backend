package com.barangay.user_management.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for editing an existing Administrator account.
 */
public record AdminUpdateRequest(

        @NotBlank(message = "System email is required.")
        @Email(message = "Please provide a valid email address.")
        @Size(max = 100, message = "Email cannot exceed 100 characters.")
        String systemEmail,

        @Email(message = "Please provide a valid backup email address.")
        @Size(max = 100, message = "Backup email cannot exceed 100 characters.")
        String systemBackupEmail,

        Boolean isActivated
) {
}
