package com.barangay.people.officer.dto;

import com.barangay.zshared.enumerated.OfficerStatus;
import jakarta.validation.constraints.NotNull;

public record OfficerStatusUpdateRequest(
        @NotNull(message = "Officer status is required")
        OfficerStatus status
) {}
