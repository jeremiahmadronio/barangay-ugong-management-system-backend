package com.barangay.usermanagement.user.service;

import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.clearance.repository.IssuedCertificateRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.audit.repository.AuditLogsRepository;
import com.barangay.zshared.dto.AdminDashboardDTO;
import com.barangay.zshared.dto.BarangayAdminDashboardKpiDTO;
import com.barangay.zshared.dto.DepartmentWeeklyOverviewDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that compiles high-level administrative dashboard statistics,
 * server health metrics (CPU and Memory), and barangay operational KPIs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final ResidentRepository residentRepository;
    private final OfficerRepository officerRepository;
    private final AuditLogsRepository auditLogsRepository;
    private final IssuedCertificateRepository issuedCertificateRepository;
    private final CaseRepository caseRepository;
    private final FirstTimeJobSeekerRepository firstTimeJobSeekerRepository;

    /**
     * Gathers comprehensive system performance metrics, hardware resource usage,
     * six-month registration trends, and recent audit activity logs.
     *
     * @return an {@link AdminDashboardDTO} containing server health and user counts.
     */
    @Transactional(readOnly = true)
    public AdminDashboardDTO getDashboardStats() {
        long totalUsers = userRepository.countTotalUsers();
        long totalResidents = residentRepository.countTotalResidents();
        long totalOfficers = officerRepository.countTotalOfficers();
        long totalAuditEntries = auditLogsRepository.count();

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

        List<Map<String, Object>> userTrend = generateSixMonthsTrend();
        List<Map<String, Object>> recentActions = getRecentActions();

        return AdminDashboardDTO.builder()
                .totalUsers(totalUsers)
                .totalResidents(totalResidents)
                .totalOfficers(totalOfficers)
                .totalAuditEntries(totalAuditEntries)
                .cpuUsage(cpuUsage)
                .memoryUsagePercent(memoryUsagePercent)
                .usedMemoryMb(usedMemoryMb)
                .totalMemoryMb(totalMemoryMb)
                .diskUsagePercent(diskUsagePercent)
                .diskFreeGb(diskFreeGb)
                .diskTotalGb(diskTotalGb)
                .userTrend(userTrend)
                .recentActions(recentActions)
                .healthStatus("HEALTHY")
                .build();
    }

    /**
     * Gathers weekly operational performance indicators across barangay departments,
     * including issued clearances, filed blotter cases, lupon sessions, and FTJS records.
     *
     * @return a {@link BarangayAdminDashboardKpiDTO} with departmental breakdown.
     */
    @Transactional(readOnly = true)
    public BarangayAdminDashboardKpiDTO getDashboardKpiStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay();

        long totalUsers = userRepository.countTotalUsers();
        long totalResidents = residentRepository.countTotalResidents();
        long totalOfficers = officerRepository.countTotalOfficers();

        long servicesIssuedThisWeek = 0;
        try {
            servicesIssuedThisWeek = issuedCertificateRepository.countWeeklyIssuedCertificates(startOfWeek, now);
        } catch (Exception e) {
            log.warn("Failed to count clearances issued this week: {}", e.getMessage());
        }

        long blotterCount = countDepartmentCases("blotter", startOfWeek, now);
        long luponCount = countDepartmentCases("lupon", startOfWeek, now);
        long bcpcCount = countDepartmentCases("bcpc", startOfWeek, now);
        long vawcCount = countDepartmentCases("vawc", startOfWeek, now);
        long clearanceCount = servicesIssuedThisWeek;

        long ftjsCount = 0;
        try {
            ftjsCount = firstTimeJobSeekerRepository.countWeeklyJobSeekers(startOfWeek, now);
        } catch (Exception e) {
            log.warn("Failed to count ftjs records this week: {}", e.getMessage());
        }

        long totalWeeklyOperations = blotterCount + luponCount + bcpcCount + vawcCount + clearanceCount + ftjsCount;

        DepartmentWeeklyOverviewDTO departmentOverview = DepartmentWeeklyOverviewDTO.builder()
                .blotter(blotterCount)
                .lupon(luponCount)
                .bcpc(bcpcCount)
                .vawc(vawcCount)
                .clearance(clearanceCount)
                .ftjs(ftjsCount)
                .totalWeeklyOperations(totalWeeklyOperations)
                .build();

        return BarangayAdminDashboardKpiDTO.builder()
                .totalUsers(totalUsers)
                .totalResidents(totalResidents)
                .totalOfficers(totalOfficers)
                .servicesIssuedThisWeek(servicesIssuedThisWeek)
                .departmentOverview(departmentOverview)
                .build();
    }

    private long countDepartmentCases(String deptName, LocalDateTime startOfWeek, LocalDateTime now) {
        try {
            return caseRepository.countWeeklyDepartmentCases(deptName, startOfWeek, now);
        } catch (Exception e) {
            log.warn("Failed to count {} cases this week: {}", deptName, e.getMessage());
            return 0;
        }
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
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM");

        for (int i = 5; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            LocalDateTime start = monthDate.withDayOfMonth(1).atStartOfDay();
            LocalDateTime end = monthDate.withDayOfMonth(monthDate.lengthOfMonth()).atTime(23, 59, 59);

            long usersCount = userRepository.countNonAdminUsersCreatedBetween(start, end);
            long residentsCount = residentRepository.countResidentsCreatedBetween(start, end);

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", monthDate.format(monthFmt));
            entry.put("users", usersCount);
            entry.put("residents", residentsCount);
            trend.add(entry);
        }

        return trend;
    }

    private List<Map<String, Object>> getRecentActions() {
        try {
            return auditLogsRepository.findByOrderByCreatedAtDesc(PageRequest.of(0, 5))
                    .stream()
                    .map(logItem -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", logItem.getId());
                        map.put("action", logItem.getActionTaken());
                        map.put("user", logItem.getUser() != null ? logItem.getUser().getUsername() : "System");
                        map.put("module", logItem.getModule() != null ? logItem.getModule().name() : "GENERAL");
                        map.put("severity", logItem.getSeverity() != null ? logItem.getSeverity().name() : "INFO");
                        map.put("timestamp", logItem.getCreatedAt() != null ? logItem.getCreatedAt().toString() : "N/A");
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to retrieve recent actions: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
