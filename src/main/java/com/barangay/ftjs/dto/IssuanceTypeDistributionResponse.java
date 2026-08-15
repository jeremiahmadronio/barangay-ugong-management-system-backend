package com.barangay.ftjs.dto;

import java.util.List;

public record IssuanceTypeDistributionResponse(
        List<IssuanceTypePointDto> points,
        long totalVolume
) {}
