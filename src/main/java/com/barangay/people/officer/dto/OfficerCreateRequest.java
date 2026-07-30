package com.barangay.people.officer.dto;

import com.barangay.zshared.enumerated.OfficerStatus;
import jakarta.validation.constraints.NotNull;

public record OfficerCreateRequest(
        @NotNull(message = "Resident/Person ID is required")
        Long personId,

        Long departmentId,

        Boolean isGlobal,

        @NotNull(message = "Position title is required")
        String position,

        OfficerStatus status
) {}
