package com.barangay.cases.blotter.dto;

import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import java.time.LocalDateTime;

/**
 * Response DTO record for Blotter Case details and table records.
 */
public record BlotterCaseResponse(
        Long id,
        String caseNumber,
        FilingType filingType,
        CaseStatus caseStatus,

        String complainantName,
        String complainantContact,
        String complainantAddress,

        String respondentName,
        String respondentContact,
        String respondentAddress,
        String relationshipToComplainant,

        String natureOfComplaint,
        String dateOfIncident,
        String timeOfIncident,
        String incidentLocation,
        String injuriesDamagesDescription,

        String statementText,
        String statementSummary,
        String receivingOfficer,
        String assignedOfficer,
        Integer witnessCount,

        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String dateFiledFormatted
) {
}
