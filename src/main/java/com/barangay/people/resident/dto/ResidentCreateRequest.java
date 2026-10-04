package com.barangay.people.resident.dto;

import com.barangay.zshared.enumerated.ResidentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Request payload DTO for registering a new Resident.
 */
public record ResidentCreateRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters.")
        @Pattern(regexp = "^[a-zA-Z\\s\\-'.]*$", message = "First name can only contain letters, spaces, hyphens, and apostrophes.")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name must not exceed 50 characters.")
        @Pattern(regexp = "^[a-zA-Z\\s\\-'.]*$", message = "Last name can only contain letters, spaces, hyphens, and apostrophes.")
        String lastName,
        String middleName,
        String suffix,
        String contactNumber,
        String email,
        String completeAddress,
        String gender,
        String civilStatus,
        String occupation,
        LocalDate birthDate,
        Short age,

        String barangayIdNumber,
        String householdNumber,
        Boolean isHeadOfFamily,
        String educationalAttainment,
        String citizenship,
        LocalDate dateOfResidency,
        ResidentStatus status,

        String photoBase64,
        List<FamilyAssociationRequest> familyAssociations,
        List<ResidentFileRequest> attachedFiles
) {
}
