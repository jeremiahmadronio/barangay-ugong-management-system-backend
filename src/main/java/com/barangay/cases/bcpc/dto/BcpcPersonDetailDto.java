package com.barangay.cases.bcpc.dto;

import java.time.LocalDate;

/**
 * Detailed view representation of a person linked to a BCPC case (e.g. child or guardian).
 */
public record BcpcPersonDetailDto(
        Long personId,
        String firstName,
        String middleName,
        String lastName,
        String fullName,
        String suffix,
        String contactNumber,
        String completeAddress,
        Short age,
        String gender,
        LocalDate birthDate,
        String civilStatus,
        String email,
        Boolean isResident
) {}

