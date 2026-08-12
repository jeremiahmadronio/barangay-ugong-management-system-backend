package com.barangay.ftjs.dto;

import java.time.LocalDateTime;

/**
 * Request payload for creating a new First Time Job Seeker (FTJS) application entry.
 */
public record FtjsRequestEntry(
        Long residentId,
        String ftjsNumber,
        LocalDateTime dateFiled,
        String releasedBy,
        String purpose,
        String educationalAttainment,
        String gradeLevel,
        String institution,
        byte[] validId,
        byte[] oathTaking
) {
}
