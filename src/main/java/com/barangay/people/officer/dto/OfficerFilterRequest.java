package com.barangay.people.officer.dto;

import com.barangay.zshared.enumerated.OfficerStatus;

public record OfficerFilterRequest(
        String search,
        String departmentName,
        OfficerStatus status
) {}
