package com.barangay.ftjs.dto;

import java.util.List;

public record TrendReportResponse(
        String granularityLabel, // "Daily" o "Monthly"
        String rangeLabel,
        List<TrendPointDto> points,
        TrendPointDto busiest,
        TrendPointDto slowest
) {}