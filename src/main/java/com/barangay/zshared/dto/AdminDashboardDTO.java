package com.barangay.zshared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDTO {

    // 1. KPI Stats Cards
    private long totalUsers;
    private long totalResidents;
    private long totalOfficers;
    private long totalAuditEntries;

    // 2. System Health Stats
    private Double cpuUsage;
    private Double usedMemoryMb;
    private Double totalMemoryMb;
    private Double memoryUsagePercent;
    private Double diskFreeGb;
    private Double diskTotalGb;
    private Double diskUsagePercent;
    private String healthStatus;

    // 3. User Growth Trend (Last 6 Months)
    private List<Map<String, Object>> userTrend;

    // 4. Recent System Actions
    private List<Map<String, Object>> recentActions;
}
