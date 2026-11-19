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
        when(userRepository.countNonAdminUsersCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(10L);
        when(residentRepository.countResidentsCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(20L);
        when(auditLogsRepository.findByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(Collections.emptyList());

        // Act
        AdminDashboardDTO result = adminDashboardService.getDashboardStats();

        // Assert
        assertNotNull(result);
        assertEquals(150L, result.getTotalUsers());
        assertEquals(5000L, result.getTotalResidents());
        assertEquals(25L, result.getTotalOfficers());
        assertEquals(1200L, result.getTotalAuditEntries());
        assertNotNull(result.getHealthStatus());

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
        when(userRepository.countNonAdminUsersCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(residentRepository.countResidentsCreatedBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(auditLogsRepository.findByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(Collections.emptyList());

        // Act
        AdminDashboardDTO result = adminDashboardService.getDashboardStats();

        // Assert
        assertAll("Zero counts validation",
                () -> assertNotNull(result),
                () -> assertEquals(0L, result.getTotalUsers()),
                () -> assertEquals(0L, result.getTotalResidents()),
                () -> assertEquals(0L, result.getTotalOfficers()),
                () -> assertEquals(0L, result.getTotalAuditEntries())
        );
    }

    @Test
    @DisplayName("getDashboardKpiStats - Happy path returns weekly operational overview")
    void getDashboardKpiStats_ValidData_ReturnsOperationalKpis() {
        // Arrange
        when(userRepository.countTotalUsers()).thenReturn(100L);
        when(residentRepository.countTotalResidents()).thenReturn(2000L);
        when(officerRepository.countTotalOfficers()).thenReturn(10L);
        when(issuedCertificateRepository.countWeeklyIssuedCertificates(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(15L);
        when(caseRepository.countWeeklyDepartmentCases(anyString(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(3L);
        when(firstTimeJobSeekerRepository.countWeeklyJobSeekers(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(5L);

        // Act
        BarangayAdminDashboardKpiDTO result = adminDashboardService.getDashboardKpiStats();

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getTotalUsers());
        assertEquals(2000L, result.getTotalResidents());
        assertEquals(10L, result.getTotalOfficers());
        assertEquals(15L, result.getServicesIssuedThisWeek());
        assertNotNull(result.getDepartmentOverview());
        assertEquals(15L, result.getDepartmentOverview().getClearance());
        assertEquals(5L, result.getDepartmentOverview().getFtjs());

        verify(issuedCertificateRepository, atLeastOnce()).countWeeklyIssuedCertificates(any(LocalDateTime.class), any(LocalDateTime.class));
        verify(caseRepository, atLeastOnce()).countWeeklyDepartmentCases(anyString(), any(LocalDateTime.class), any(LocalDateTime.class));
    }
}
