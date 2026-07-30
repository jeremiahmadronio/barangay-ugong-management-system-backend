package com.barangay.events.dto;

import jakarta.validation.constraints.NotBlank;

public record EventStatusUpdateRequest(
        @NotBlank(message = "Status is required")
        String status
) {}
