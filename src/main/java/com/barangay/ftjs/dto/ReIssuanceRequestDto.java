package com.barangay.ftjs.dto;

import java.time.LocalDate;

/**
 * DTO request payload for creating FTJS certificate re-issuance requests.
 */
public record ReIssuanceRequestDto(
        Long ftjsId,
        String reason,
        LocalDate dateOfLoss,
        String orNumber,
        byte[] affidavit,
        Long blotterCaseId
) {
    public ReIssuanceRequestDto(Long ftjsId, String reason, LocalDate dateOfLoss, String orNumber, byte[] affidavit) {
        this(ftjsId, reason, dateOfLoss, orNumber, affidavit, null);
    }
}
