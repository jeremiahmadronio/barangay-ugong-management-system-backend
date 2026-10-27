package com.barangay.cases.blotter.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyDocumentReissuanceDTO(
        @NotBlank(message = "Action is required (APPROVE or REJECT)")
        String action,
        String remarks
) {}
