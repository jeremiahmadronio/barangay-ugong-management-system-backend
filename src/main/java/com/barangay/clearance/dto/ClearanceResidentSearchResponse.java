package com.barangay.clearance.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Detailed resident search response for clearance issuance, including demographic info,
 * clearance issuance history, and multi-department case history (BCPC, VAWC, Lupon, Blotter).
 */
public record ClearanceResidentSearchResponse(
        Long residentId,
        Long personId,
        String firstName,
        String middleName,
        String lastName,
        String suffix,
        String fullName,
        Integer age,
        String gender,
        String civilStatus,
        String contactNumber,
        String completeAddress,
        LocalDate birthDate,
        String placeOfBirth,
        String residencyDate,
        List<ClearanceIssuanceHistoryResponse> issuanceHistory,
        List<ClearanceCaseHistoryResponse> caseHistory
) {
}
