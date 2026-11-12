package com.barangay.kapitana.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KapitanaDepartmentOverviewResponse {
    private KapitanaDepartmentMetrics blotter;
    private KapitanaDepartmentMetrics lupon;
    private KapitanaDepartmentMetrics vawc;
    private KapitanaDepartmentMetrics bcpc;
    private long clearanceTotalIssued;
    private long ftjsTotalIssued;
}
