package com.barangay.usermanagement.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for partially updating personal information.
 * Only altered fields are updated in the database; unchanged fields remain untouched.
 * System email is strictly read-only and immutable.
 */
public record UpdatePersonalInfoRequest(

        @Size(max = 50, message = "First name must not exceed 50 characters.")
        @Pattern(regexp = "^[a-zA-Z\\s\\-'.]*$", message = "First name can only contain letters, spaces, hyphens, and apostrophes.")
        String firstName,

        @Size(max = 50, message = "Last name must not exceed 50 characters.")
        @Pattern(regexp = "^[a-zA-Z\\s\\-'.]*$", message = "Last name can only contain letters, spaces, hyphens, and apostrophes.")
        String lastName,

        @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters.")
        @Pattern(regexp = "^[a-zA-Z0-9_.]*$", message = "Username can only contain alphanumeric characters, underscores, and dots.")
        String username,

        @Size(max = 20, message = "Contact number must not exceed 20 characters.")
        @Pattern(regexp = "^[0-9+\\-\\s()]*$", message = "Contact number can only contain numbers and valid phone symbols (+, -, space).")
        String contactNumber
) {
}
