package com.barangay.ftjs.dto;

public record IssuanceTypePointDto(
        String typeLabel,
        long count,
        double share
) {}
