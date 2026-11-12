package com.barangay.kapitana.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KapitanaDashboardKpiResponse {
    private long totalActiveCases;
    private long totalSettledCases;
    private long totalCasesAcrossDepartments;
    private double overallResolutionRate;
    private long totalClearancesIssued;
    private long totalFtjsIssued;
    private KapitanaDepartmentOverviewResponse departmentOverview;
    private KapitanaDemographicsResponse demographics;
}
