package com.barangay.cases.blotter.dto;

import com.barangay.zshared.enumerated.FilingType;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Request payload DTO for creating a new Blotter Case entry (Formal Complaint or For the Record).
 */
public record BlotterCaseCreateRequest(
        FilingType filingType,
        String caseNumber,
        String dateFiled,
        String timeFiled,
        String receivingOfficer,
        String assignedOfficer,

        ComplainantDto complainant,
        RespondentDto respondent,
        IncidentDto incident,

        String statementText,
        String statementSummary,
        List<WitnessDto> witnesses,
        Boolean isCertified
) {
    public record ComplainantDto(
            @NotBlank(message = "Complainant last name is required")
            String lastName,
            @NotBlank(message = "Complainant first name is required")
            String firstName,
            String middleName,
            String contactNumber,
            Short age,
            String gender,
            String civilStatus,
            String email,
            String houseStreet,
            String barangay,
            String city,
            String province,
            String completeAddress
    ) {}

    public record RespondentDto(
            @NotBlank(message = "Respondent last name is required")
            String lastName,
            @NotBlank(message = "Respondent first name is required")
            String firstName,
            String middleName,
            String contactNumber,
            Short age,
            String gender,
            String houseStreet,
            String barangay,
            String city,
            String province,
            String completeAddress,
            String relationshipToComplainant,
            Boolean currentlyLivingWithComplainant
    ) {}

    public record IncidentDto(
            @NotBlank(message = "Nature of complaint is required")
            String natureOfComplaint,
            String dateOfIncident,
            String timeOfIncident,
            @NotBlank(message = "Location of incident is required")
            String placeOfIncident,
            String frequencyOfIncident,
            String descriptionOfInjuries
    ) {}

    public record WitnessDto(
            String firstName,
            String lastName,
            String contactNumber,
            String houseStreet,
            String barangay,
            String city,
            String province,
            String completeAddress,
            String testimonyText
    ) {}
}
