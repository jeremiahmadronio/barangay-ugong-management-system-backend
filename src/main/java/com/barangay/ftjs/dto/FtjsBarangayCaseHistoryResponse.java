package com.barangay.ftjs.dto;

import java.time.LocalDateTime;

public record FtjsBarangayCaseHistoryResponse(
        String caseNumber,
        String natureOfComplaint,
        String role,
        LocalDateTime dateFiled,
        String status
) {
}
