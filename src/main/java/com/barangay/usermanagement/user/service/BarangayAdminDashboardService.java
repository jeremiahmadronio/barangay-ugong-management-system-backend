package com.barangay.usermanagement.user.service;

import com.barangay.zshared.dto.BarangayAdminDashboardKpiDTO;
import com.barangay.zshared.dto.DepartmentWeeklyOverviewDTO;
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
        LocalDateTime startOfWeek = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        // 1. Total System Users Only
        long totalUsers = 0;
        try {
            String jpqlUsers = "SELECT COUNT(u) FROM User u LEFT JOIN u.role r " + SYSTEM_USER_FILTER;
            totalUsers = entityManager.createQuery(jpqlUsers, Long.class).getSingleResult();
        } catch (Exception e) {
            log.warn("Failed to count system users: {}", e.getMessage());
        }

        // 2. Total Registered Residents (Not soft-deleted, not permanently deleted, not archived)
        long totalResidents = 0;
        try {
            String jpqlResidents = "SELECT COUNT(r) FROM Resident r " +
                    " WHERE (r.isDeleted = false OR r.isDeleted IS NULL) " +
                    " AND (r.isPermanentDeleted = false OR r.isPermanentDeleted IS NULL) " +
                    " AND (r.status IS NULL OR r.status != com.barangay.zshared.enumerated.ResidentStatus.ARCHIVED) ";
            totalResidents = entityManager.createQuery(jpqlResidents, Long.class).getSingleResult();
        } catch (Exception e) {
            log.warn("Failed to count residents: {}", e.getMessage());
        }

        // 3. Total Registered Officers (Not soft-deleted, not permanently deleted, not archived)
        long totalOfficers = 0;
        try {
            String jpqlOfficers = "SELECT COUNT(o) FROM Officer o " +
                    " WHERE (o.isDeleted = false OR o.isDeleted IS NULL) " +
                    " AND (o.isPermanentDeleted = false OR o.isPermanentDeleted IS NULL) " +
                    " AND (o.status IS NULL OR o.status != com.barangay.zshared.enumerated.OfficerStatus.ARCHIVED) ";
            totalOfficers = entityManager.createQuery(jpqlOfficers, Long.class).getSingleResult();
        } catch (Exception e) {
            log.warn("Failed to count officers: {}", e.getMessage());
        }

        // 4. Total Services Issued This Week (Barangay Clearances created this week)
        long servicesIssuedThisWeek = 0;
        try {
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

        // 5. Department Overview Operations (This Week Only)
        long blotterCount = 0;
        try {
            String jpql = "SELECT COUNT(c) FROM Case c LEFT JOIN c.department d " +
                    " WHERE LOWER(TRIM(d.name)) = 'blotter' " +
                    " AND c.createdAt >= :startOfWeek AND c.createdAt <= :now " +
                    " AND (c.isDeleted IS NULL OR c.isDeleted = false) " +
                    " AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) " +
                    " AND (c.isArchived IS NULL OR c.isArchived = false) ";
            blotterCount = entityManager.createQuery(jpql, Long.class)
                    .setParameter("startOfWeek", startOfWeek)
                    .setParameter("now", now)
                    .getSingleResult();
        } catch (Exception e) {
            log.warn("Failed to count blotter cases this week: {}", e.getMessage());
        }

        long luponCount = 0;
        try {
            String jpql = "SELECT COUNT(c) FROM Case c LEFT JOIN c.department d " +
                    " WHERE LOWER(TRIM(d.name)) = 'lupon' " +
                    " AND c.createdAt >= :startOfWeek AND c.createdAt <= :now " +
                    " AND (c.isDeleted IS NULL OR c.isDeleted = false) " +
                    " AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) " +
                    " AND (c.isArchived IS NULL OR c.isArchived = false) ";
            luponCount = entityManager.createQuery(jpql, Long.class)
                    .setParameter("startOfWeek", startOfWeek)
                    .setParameter("now", now)
                    .getSingleResult();
        } catch (Exception e) {
            log.warn("Failed to count lupon cases this week: {}", e.getMessage());
        }

        long bcpcCount = 0;
        try {
            String jpql = "SELECT COUNT(c) FROM Case c LEFT JOIN c.department d " +
                    " WHERE LOWER(TRIM(d.name)) = 'bcpc' " +
                    " AND c.createdAt >= :startOfWeek AND c.createdAt <= :now " +
                    " AND (c.isDeleted IS NULL OR c.isDeleted = false) " +
                    " AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) " +
                    " AND (c.isArchived IS NULL OR c.isArchived = false) ";
            bcpcCount = entityManager.createQuery(jpql, Long.class)
                    .setParameter("startOfWeek", startOfWeek)
                    .setParameter("now", now)
                    .getSingleResult();
        } catch (Exception e) {
            log.warn("Failed to count bcpc cases this week: {}", e.getMessage());
        }

        long vawcCount = 0;
        try {
            String jpql = "SELECT COUNT(c) FROM Case c LEFT JOIN c.department d " +
                    " WHERE LOWER(TRIM(d.name)) = 'vawc' " +
                    " AND c.createdAt >= :startOfWeek AND c.createdAt <= :now " +
                    " AND (c.isDeleted IS NULL OR c.isDeleted = false) " +
                    " AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) " +
                    " AND (c.isArchived IS NULL OR c.isArchived = false) ";
            vawcCount = entityManager.createQuery(jpql, Long.class)
                    .setParameter("startOfWeek", startOfWeek)
                    .setParameter("now", now)
                    .getSingleResult();
        } catch (Exception e) {
            log.warn("Failed to count vawc cases this week: {}", e.getMessage());
        }

        long clearanceCount = servicesIssuedThisWeek;

        long ftjsCount = 0;
        try {
            String jpql = "SELECT COUNT(f) FROM FirstTimeJobSeeker f " +
                    " WHERE f.createdAt >= :startOfWeek AND f.createdAt <= :now " +
                    " AND (f.isArchived IS NULL OR f.isArchived = false) " +
                    " AND (f.isDeleted IS NULL OR f.isDeleted = false) " +
                    " AND (f.isPermanentDeleted IS NULL OR f.isPermanentDeleted = false) ";
            ftjsCount = entityManager.createQuery(jpql, Long.class)
                    .setParameter("startOfWeek", startOfWeek)
                    .setParameter("now", now)
                    .getSingleResult();
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
}
