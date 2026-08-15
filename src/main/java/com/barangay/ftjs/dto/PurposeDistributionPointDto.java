package com.barangay.ftjs.dto;

public record PurposeDistributionPointDto(
        String purpose,
        long count,
        double share
) {}
