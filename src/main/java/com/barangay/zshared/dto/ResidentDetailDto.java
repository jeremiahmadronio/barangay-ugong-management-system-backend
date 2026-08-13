package com.barangay.zshared.dto;

import java.time.LocalDate;

public record ResidentDetailDto(
        Long id,
        String lastName,
        String firstName,
        String middleName,
        String fullName,
        String contactNumber,
        Short age,
        String gender,
        String civilStatus,
        String email,
        String completeAddress,
        LocalDate residencyDate,
        String residentNumber
) {
}
