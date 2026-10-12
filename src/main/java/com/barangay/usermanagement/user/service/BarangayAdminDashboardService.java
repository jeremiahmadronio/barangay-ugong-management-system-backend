package com.barangay.usermanagement.user.service;

import com.barangay.zshared.dto.BarangayAdminDashboardKpiDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

@Service
@Slf4j
@RequiredArgsConstructor
public class BarangayAdminDashboardService {

    @PersistenceContext
    private final EntityManager entityManager;

    /**
     * Common JPQL WHERE clause fragment ensuring only System Users (non-admins) are targeted.
     * Excludes root admins, admins, superadmin, isSystem, archived, soft-deleted, and permanently deleted accounts.
     */
    private static final String SYSTEM_USER_FILTER =
            " WHERE (u.isArchived = false OR u.isArchived IS NULL) " +
            " AND (u.status IS NULL OR u.status <> com.barangay.zshared.enumerated.UserStatus.ARCHIVED) " +
            " AND (u.isDeleted = false OR u.isDeleted IS NULL) " +
            " AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) " +
            " AND (u.isSystem = false OR u.isSystem IS NULL) " +
            " AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') " +
            " AND (r.roleName IS NOT NULL AND UPPER(r.roleName) NOT LIKE '%ADMIN%') ";

    @Transactional(readOnly = true)
    public BarangayAdminDashboardKpiDTO getDashboardKpiStats() {
        // 1. Total System Users Only
        long totalUsers = 0;
        try {
            String jpqlUsers = "SELECT COUNT(u) FROM User u LEFT JOIN u.role r " + SYSTEM_USER_FILTER;
            totalUsers = entityManager.createQuery(jpqlUsers, Long.class).getSingleResult();
        } catch (Exception e) {
            log.warn("Failed to count system users: {}", e.getMessage());
        }

        // 2. Total Active Residents (Not soft-deleted, not permanently deleted)
        long totalResidents = 0;
        try {
            String jpqlResidents = "SELECT COUNT(r) FROM Resident r " +
                    " WHERE (r.isDeleted = false OR r.isDeleted IS NULL) " +
                    " AND (r.isPermanentDeleted = false OR r.isPermanentDeleted IS NULL) " +
                    " AND (r.status = com.barangay.zshared.enumerated.ResidentStatus.ACTIVE) ";
            totalResidents = entityManager.createQuery(jpqlResidents, Long.class).getSingleResult();
        } catch (Exception e) {
            log.warn("Failed to count active residents: {}", e.getMessage());
        }

        // 3. Total Active Officers (Not soft-deleted, not permanently deleted)
        long totalOfficers = 0;
        try {
            String jpqlOfficers = "SELECT COUNT(o) FROM Officer o " +
                    " WHERE (o.isDeleted = false OR o.isDeleted IS NULL) " +
                    " AND (o.isPermanentDeleted = false OR o.isPermanentDeleted IS NULL) " +
                    " AND (o.status = com.barangay.zshared.enumerated.OfficerStatus.ACTIVE) ";
            totalOfficers = entityManager.createQuery(jpqlOfficers, Long.class).getSingleResult();
        } catch (Exception e) {
            log.warn("Failed to count active officers: {}", e.getMessage());
        }

        // 4. Total Services Issued This Week (Barangay Clearances created this week)
        long servicesIssuedThisWeek = 0;
        try {
            LocalDateTime startOfWeek = LocalDate.now()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay();
            LocalDateTime now = LocalDateTime.now();

            String jpqlClearance = "SELECT COUNT(ic) FROM IssuedCertificate ic " +
                    " WHERE ic.createdAt >= :startOfWeek AND ic.createdAt <= :now " +
                    " AND (ic.isArchived = false OR ic.isArchived IS NULL) " +
                    " AND (ic.isDeleted = false OR ic.isDeleted IS NULL) " +
                    " AND (ic.isPermanentDeleted = false OR ic.isPermanentDeleted IS NULL) ";
            servicesIssuedThisWeek = entityManager.createQuery(jpqlClearance, Long.class)
                    .setParameter("startOfWeek", startOfWeek)
                    .setParameter("now", now)
                    .getSingleResult();
        } catch (Exception e) {
            log.warn("Failed to count clearances issued this week: {}", e.getMessage());
        }

        return BarangayAdminDashboardKpiDTO.builder()
                .totalUsers(totalUsers)
                .totalResidents(totalResidents)
                .totalOfficers(totalOfficers)
                .servicesIssuedThisWeek(servicesIssuedThisWeek)
                .build();
    }
}
