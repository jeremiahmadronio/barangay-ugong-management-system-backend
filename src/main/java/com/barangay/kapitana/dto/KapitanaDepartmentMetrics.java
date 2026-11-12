package com.barangay.kapitana.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KapitanaDepartmentMetrics {
    private String department;
    private long activeCases;
    private long settledCases;
    private long totalCases;
    private double resolutionRate;
}
