package com.barangay.people.resident.dto;

import com.barangay.user_management.user.dto.UserDetailResponse.PersonSummaryInfo;
import com.barangay.zshared.enumerated.ResidentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed DTO response payload for viewing a complete Resident Profile.
 */
public record ResidentDetailResponse(
        Long id,
        String barangayIdNumber,
        String householdNumber,
        Boolean isHeadOfFamily,
        ResidentStatus status,
        String statusRemarks,
        String citizenship,
        String educationalAttainment,
        LocalDate dateOfResidency,
        PersonSummaryInfo personInfo,
        String photoBase64,
        List<ResidentRelativeResponse> familyAssociations,
        List<ResidentFileResponse> attachedFiles,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
