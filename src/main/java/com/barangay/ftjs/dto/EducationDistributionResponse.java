package com.barangay.ftjs.dto;

import java.util.List;

public record EducationDistributionResponse(
        List<EducationDistributionPointDto> points,
        String highestLabel,
        long highestCount,
        long totalVolume
) {}