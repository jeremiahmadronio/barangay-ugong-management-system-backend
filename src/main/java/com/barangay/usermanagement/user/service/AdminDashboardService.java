package com.barangay.usermanagement.user.service;

import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.audit.model.AuditLogs;
import com.barangay.zshared.audit.repository.AuditLogsRepository;
import com.barangay.zshared.dto.AdminDashboardDTO;
import com.barangay.zshared.enumerated.OfficerStatus;
import com.barangay.zshared.enumerated.ResidentStatus;
import com.barangay.zshared.enumerated.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final ResidentRepository residentRepository;
    private final OfficerRepository officerRepository;
    private final AuditLogsRepository auditLogsRepository;

    @Transactional(readOnly = true)
    public AdminDashboardDTO getDashboardStats() {
        // 1. KPI Stats Cards
        long totalUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long totalResidents = residentRepository.countByStatus(ResidentStatus.ACTIVE);
        long totalOfficers = officerRepository.countByStatus(OfficerStatus.ACTIVE);
        long totalAuditEntries = auditLogsRepository.count();

        // 2. System Health Stats
        double cpuUsage = getCpuUsage();

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemoryBytes = totalMemory - freeMemory;

        double usedMemoryMb = Math.round((usedMemoryBytes / (1024.0 * 1024.0)) * 100.0) / 100.0;
        double totalMemoryMb = Math.round((maxMemory / (1024.0 * 1024.0)) * 100.0) / 100.0;
        double memoryUsagePercent = totalMemoryMb > 0
                ? Math.round((usedMemoryMb / totalMemoryMb * 100.0) * 100.0) / 100.0
                : 0.0;

        File root = new File("/");
        long diskTotalBytes = root.getTotalSpace();
        long diskFreeBytes = root.getFreeSpace();

        double diskTotalGb = Math.round((diskTotalBytes / (1024.0 * 1024.0 * 1024.0)) * 100.0) / 100.0;
        double diskFreeGb = Math.round((diskFreeBytes / (1024.0 * 1024.0 * 1024.0)) * 100.0) / 100.0;
        double diskUsagePercent = diskTotalGb > 0
                ? Math.round(((diskTotalGb - diskFreeGb) / diskTotalGb * 100.0) * 100.0) / 100.0
                : 0.0;

        // 3. User Growth Trend (Last 6 Months)
        List<Map<String, Object>> userTrend = generateSixMonthsTrend();

        // 4. Recent System Actions (Top 5 Audit Logs)
        List<Map<String, Object>> recentActions = getRecentActions();

        return AdminDashboardDTO.builder()
                .totalUsers(totalUsers)
                .totalResidents(totalResidents)
                .totalOfficers(totalOfficers)
                .totalAuditEntries(totalAuditEntries)
                .cpuUsage(cpuUsage)
                .usedMemoryMb(usedMemoryMb)
                .totalMemoryMb(totalMemoryMb)
                .memoryUsagePercent(memoryUsagePercent)
                .diskFreeGb(diskFreeGb)
                .diskTotalGb(diskTotalGb)
                .diskUsagePercent(diskUsagePercent)
                .healthStatus("HEALTHY")
                .userTrend(userTrend)
                .recentActions(recentActions)
                .build();
    }

    private double getCpuUsage() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
                double load = sunOsBean.getCpuLoad();
                if (load >= 0) {
                    return Math.round((load * 100.0) * 100.0) / 100.0;
                }
            }
        } catch (Exception e) {
            log.debug("CPU load calculation fallback: {}", e.getMessage());
        }
        return 0.0;
    }

    private List<Map<String, Object>> generateSixMonthsTrend() {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM");

        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            java.time.LocalDateTime endOfMonth = (i == 0)
                    ? java.time.LocalDateTime.now()
                    : monthDate.withDayOfMonth(monthDate.lengthOfMonth()).atTime(23, 59, 59);

            String monthName = monthDate.format(formatter);

            long userCount = userRepository.countNonAdminUsersCreatedOnOrBefore(endOfMonth);
            long residentCount = residentRepository.countResidentsCreatedOnOrBefore(endOfMonth);

            Map<String, Object> point = new HashMap<>();
            point.put("month", monthName);
            point.put("users", userCount);
            point.put("residents", residentCount);
            trend.add(point);
        }

        return trend;
    }

    private List<Map<String, Object>> getRecentActions() {
        List<Map<String, Object>> list = new ArrayList<>();
        try {
            List<AuditLogs> logs = auditLogsRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, 5));
            for (AuditLogs audit : logs) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", audit.getId());
                map.put("user", audit.getUser() != null ? audit.getUser().getUsername() : "System");
                map.put("action", audit.getActionTaken());
                map.put("module", audit.getModule() != null ? audit.getModule().name() : "SYSTEM");
                map.put("timestamp", audit.getCreatedAt() != null ? audit.getCreatedAt().toString() : "");
                list.add(map);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch recent actions: {}", e.getMessage());
        }
        return list;
    }
}
