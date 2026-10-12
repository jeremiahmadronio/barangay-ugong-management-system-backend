package com.barangay.zshared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarangayAdminDashboardKpiDTO {
    private long totalUsers;
    private long totalResidents;
    private long totalOfficers;
    private long servicesIssuedThisWeek;
    private DepartmentWeeklyOverviewDTO departmentOverview;
}
