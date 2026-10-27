package com.barangay.cases.blotter.dto;

import java.time.LocalDate;

public record FtjsSearchDTO(
        Long id,
        String ftjsNumber,
        String applicantName,
        String firstName,
        String middleName,
        String lastName,
        String suffix,
        String contactNumber,
        String completeAddress,
        String email,
        String civilStatus,
        Integer age,
        String gender,
        Long residentId,
        Long personId,
        LocalDate dateSubmitted,
        Integer issuanceCount,
        boolean hasActiveBlotter,
        String activeBlotterNumber,
        String activeBlotterStatus
) {}
