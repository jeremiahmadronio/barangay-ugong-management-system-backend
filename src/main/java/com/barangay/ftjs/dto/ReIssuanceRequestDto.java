package com.barangay.ftjs.dto;

import java.time.LocalDate;

public record ReIssuanceRequestDto(

        Long ftjsId,
        String reason,
        LocalDate dateOfLoss,
        String orNumber,
        byte[] affidavit
) {
}
