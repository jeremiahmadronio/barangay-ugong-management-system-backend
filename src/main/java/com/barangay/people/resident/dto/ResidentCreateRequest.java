package com.barangay.people.resident.dto;

import com.barangay.zshared.enumerated.ResidentStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

/**
 * Request payload DTO for registering a new Resident.
 */
public record ResidentCreateRequest(
        @NotBlank(message = "First name is required")
        String firstName,
        @NotBlank(message = "Last name is required")
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
