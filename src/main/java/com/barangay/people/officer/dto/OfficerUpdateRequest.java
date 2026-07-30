package com.barangay.people.officer.dto;

import com.barangay.zshared.enumerated.OfficerStatus;

public record OfficerUpdateRequest(
        Long departmentId,
        Boolean isGlobal,
        String position,
        OfficerStatus status
) {}
