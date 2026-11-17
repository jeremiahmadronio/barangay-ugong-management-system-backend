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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private OfficerRepository officerRepository;

    @Mock
    private AuditLogsRepository auditLogsRepository;

    @Mock
    private IssuedCertificateRepository issuedCertificateRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private FirstTimeJobSeekerRepository firstTimeJobSeekerRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    @DisplayName("getDashboardStats - Happy path returns aggregated metrics and system resource stats")
    void getDashboardStats_ValidData_ReturnsAggregatedMetrics() {
        // Arrange
        when(userRepository.countTotalUsers()).thenReturn(150L);
        when(residentRepository.countTotalResidents()).thenReturn(5000L);
        when(officerRepository.countTotalOfficers()).thenReturn(25L);
        when(auditLogsRepository.count()).thenReturn(1200L);
        when(userRepository.countTotalUsersBetween(any(LocalDateTime.class), any(LocalDateTime.class), any())).thenReturn(10L);
        when(residentRepository.countTotalResidentsBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(20L);
        when(officerRepository.countTotalOfficersBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(2L);
        when(auditLogsRepository.countAuditLogsBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(50L);
        when(auditLogsRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(Collections.emptyList());

        // Act
        AdminDashboardDTO result = adminDashboardService.getDashboardStats();

        // Assert
        assertNotNull(result);
        assertEquals(150L, result.totalUsers());
        assertEquals(5000L, result.totalResidents());
        assertEquals(25L, result.totalOfficers());
        assertEquals(1200L, result.totalAuditLogs());
        assertNotNull(result.systemHealth());

        verify(userRepository, times(1)).countTotalUsers();
        verify(residentRepository, times(1)).countTotalResidents();
        verify(officerRepository, times(1)).countTotalOfficers();
        verify(auditLogsRepository, times(1)).count();
    }

    @Test
    @DisplayName("getDashboardStats - When repositories return zero counts")
    void getDashboardStats_EmptyData_ReturnsZeroMetricsWithoutException() {
        // Arrange
        when(userRepository.countTotalUsers()).thenReturn(0L);
        when(residentRepository.countTotalResidents()).thenReturn(0L);
        when(officerRepository.countTotalOfficers()).thenReturn(0L);
        when(auditLogsRepository.count()).thenReturn(0L);
        when(userRepository.countTotalUsersBetween(any(LocalDateTime.class), any(LocalDateTime.class), any())).thenReturn(0L);
        when(residentRepository.countTotalResidentsBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(officerRepository.countTotalOfficersBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(auditLogsRepository.countAuditLogsBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(auditLogsRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(Collections.emptyList());

        // Act
        AdminDashboardDTO result = adminDashboardService.getDashboardStats();

        // Assert
        assertAll("Zero counts validation",
                () -> assertNotNull(result),
                () -> assertEquals(0L, result.totalUsers()),
                () -> assertEquals(0L, result.totalResidents()),
                () -> assertEquals(0L, result.totalOfficers()),
                () -> assertEquals(0L, result.totalAuditLogs())
        );
    }

    @Test
    @DisplayName("getDashboardKpiStats - Happy path returns weekly operational overview")
    void getDashboardKpiStats_ValidData_ReturnsOperationalKpis() {
        // Arrange
        when(caseRepository.countCasesCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(5L);
        when(issuedCertificateRepository.countCertificatesCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(12L);
        when(firstTimeJobSeekerRepository.countApplicationsCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(3L);
        when(caseRepository.countByIsDeletedFalseAndIsPermanentDeletedFalse()).thenReturn(40L);
        when(issuedCertificateRepository.countByIsDeletedFalseAndIsPermanentDeletedFalse()).thenReturn(150L);
        when(firstTimeJobSeekerRepository.countByIsDeletedFalseAndIsPermanentDeletedFalse()).thenReturn(20L);

        // Act
        BarangayAdminDashboardKpiDTO result = adminDashboardService.getDashboardKpiStats();

        // Assert
        assertNotNull(result);
        assertEquals(40L, result.totalActiveCases());
        assertEquals(150L, result.totalIssuedClearances());
        assertEquals(20L, result.totalFtjsApplications());
        assertNotNull(result.weeklyOverview());
        assertFalse(result.weeklyOverview().isEmpty());

        verify(caseRepository, atLeastOnce()).countCasesCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(issuedCertificateRepository, atLeastOnce()).countCertificatesCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }
}
