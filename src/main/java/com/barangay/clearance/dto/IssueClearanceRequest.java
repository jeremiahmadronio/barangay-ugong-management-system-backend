package com.barangay.clearance.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for issuing a clearance or certificate.
 */
public record IssueClearanceRequest(
        @NotNull(message = "Template ID is required")
        Long templateId,
        Long personId,
        String requestorName,
        JsonNode fieldValues,
        String orNumber,
        String ctnNumber,
        String remarks
) {}
