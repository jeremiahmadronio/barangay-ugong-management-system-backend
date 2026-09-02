package com.barangay.clearance.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed response for an individual issued certificate, including applicant demographics,
 * issuing officer info, payment, signatory, and dynamic form submission values.
 */
public record IssuedCertificateDetailResponse(
        Long id,
        String certNumber,
        Long templateId,
        String certTitle,
        String templateCode,
        String purpose,
        String orNumber,
        String ctnNumber,
        BigDecimal amountPaid,
        boolean isFree,
        String signatoryName,
        String signatoryPosition,
        String status,
        LocalDate expiryDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        JsonNode fieldValues,
        Boolean isArchived,
        LocalDateTime archivedAt,
        String archiveRemarks,
        ClearanceOfficerSummary issuingOfficer,
        ClearanceApplicantSummary applicant,
        int totalIssuances,
        List<IssuedCertificateHistoryItem> history
) {

    public record ClearanceOfficerSummary(
            Long id,
            String fullName,
            String username,
            String roleName,
            String employeeId
    ) {}

    public record ClearanceApplicantSummary(
            Long personId,
            Long residentId,
            String firstName,
            String middleName,
            String lastName,
            String suffix,
            String fullName,
            Integer age,
            String gender,
            String civilStatus,
            String contactNumber,
            String email,
            String completeAddress,
            LocalDate birthDate,
            String placeOfBirth,
            String citizenship,
            String occupation,
            String barangayIdNumber,
            boolean isResident
    ) {}
}
