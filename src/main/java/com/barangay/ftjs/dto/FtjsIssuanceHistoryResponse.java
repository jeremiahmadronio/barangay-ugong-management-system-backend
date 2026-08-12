package com.barangay.ftjs.dto;

import java.time.LocalDateTime;

public record FtjsIssuanceHistoryResponse(
        String ftjsNumber,
        LocalDateTime issuedAt,
        String status
) {
}
