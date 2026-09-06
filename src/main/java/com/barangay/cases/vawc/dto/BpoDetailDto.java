package com.barangay.cases.vawc.dto;

import com.barangay.zshared.enumerated.BpoStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BpoDetailDto(
        Long id,
        String bpoControlNumber,
        BpoStatus status,
        LocalDateTime activatedAt,
        LocalDate expiredAt,
        String servedBy,
        LocalDateTime dateServed,
        String serviceMethod,
        String witness1Name,
        String witness2Name,
        String certificationFileName,
        String certificationFileData
) {}
