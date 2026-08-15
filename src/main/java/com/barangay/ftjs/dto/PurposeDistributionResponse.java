package com.barangay.ftjs.dto;

import java.util.List;

public record PurposeDistributionResponse(
        List<PurposeDistributionPointDto> points,
        long totalVolume
) {}
