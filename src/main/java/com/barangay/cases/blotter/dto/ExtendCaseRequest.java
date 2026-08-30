package com.barangay.cases.blotter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExtendCaseRequest(
        @NotBlank(message = "Extension reason is required")
        @Size(max = 1000, message = "Extension reason must not exceed 1000 characters")
        String reason
) {}
