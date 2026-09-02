package com.barangay.clearance.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;

/**
 * Detailed DTO for a specific clearance template when loaded into the issuance form.
 */
public record TemplateDetailResponse(
        Long id,
        String certTitle,
        JsonNode bodySections,
        JsonNode issueFields,
        boolean requiresPhoto,
        boolean requiresThumbmark,
        Integer validityMonths,
        String footerText,
        Boolean isArchived
) {}
