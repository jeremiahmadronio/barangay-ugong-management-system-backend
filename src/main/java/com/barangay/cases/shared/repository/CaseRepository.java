package com.barangay.cases.shared.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import com.barangay.cases.shared.projection.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.barangay.cases.shared.model.Case;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;

/**
 * Access point for Case data combining Spring Data JPA with CaseRepositoryCustom.
 */
@Repository
public interface CaseRepository extends JpaRepository<Case, Long>, CaseRepositoryCustom {

    /**
     * Efficient JPQL query to retrieve all cases where a person is involved as Complainant, Respondent, or Witness.
     * Uses indexes on person_id. Zero findAll() operations.
     */
    @Query("SELECT DISTINCT c FROM Case c LEFT JOIN c.witnesses w WHERE (c.complainant IS NOT NULL AND c.complainant.person.id = :personId) OR (c.respondent IS NOT NULL AND c.respondent.person.id = :personId) OR (w.person IS NOT NULL AND w.person.id = :personId) ORDER BY c.createdAt DESC")
    List<Case> findByPersonIdInCaseRoles(@Param("personId") Long personId);

    /**
     * Checks whether a case with the given case number already exists in the database.
     *
     * @param caseNumber the case number to check
     * @return true if a case with the case number exists, false otherwise
     */
    boolean existsByCaseNumber(String caseNumber);

    /**
     * Finds a case by its case number.
     *
     * @param caseNumber the case number
     * @return Optional containing the Case if found
     */
    java.util.Optional<Case> findByCaseNumber(String caseNumber);

    // Root Case KPI Count queries for current month vs previous month trends
    @Query("SELECT COUNT(c) FROM Case c WHERE (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') AND (c.isArchived IS NULL OR c.isArchived = false) AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)")
    long countRootCases();

    @Query("SELECT COUNT(c) FROM Case c WHERE (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') AND (c.isArchived IS NULL OR c.isArchived = false) AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND c.createdAt >= :start")
    long countRootCasesByCreatedAtGreaterThanEqual(@Param("start") LocalDateTime start);

    @Query("SELECT COUNT(c) FROM Case c WHERE (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') AND (c.isArchived IS NULL OR c.isArchived = false) AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND c.createdAt BETWEEN :start AND :end")
    long countRootCasesByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(c) FROM Case c WHERE (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') AND (c.isArchived IS NULL OR c.isArchived = false) AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND c.filingType = :filingType")
    long countRootCasesByFilingType(@Param("filingType") FilingType filingType);

    @Query("SELECT COUNT(c) FROM Case c WHERE (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') AND (c.isArchived IS NULL OR c.isArchived = false) AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND c.filingType = :filingType AND c.createdAt >= :start")
    long countRootCasesByFilingTypeAndCreatedAtGreaterThanEqual(@Param("filingType") FilingType filingType, @Param("start") LocalDateTime start);

    @Query("SELECT COUNT(c) FROM Case c WHERE (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') AND (c.isArchived IS NULL OR c.isArchived = false) AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND c.filingType = :filingType AND c.createdAt BETWEEN :start AND :end")
    long countRootCasesByFilingTypeAndCreatedAtBetween(@Param("filingType") FilingType filingType, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(c) FROM Case c WHERE (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '') AND (c.isArchived IS NULL OR c.isArchived = false) AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND c.caseStatus IN :statuses")
    long countLatestCasesByCaseStatusIn(@Param("statuses") Collection<CaseStatus> statuses);

    @Query("SELECT COUNT(c) FROM Case c WHERE (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '') AND (c.isArchived IS NULL OR c.isArchived = false) AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND c.caseStatus IN :statuses AND c.createdAt >= :start")
    long countLatestCasesByCaseStatusInAndCreatedAtGreaterThanEqual(@Param("statuses") Collection<CaseStatus> statuses, @Param("start") LocalDateTime start);

    @Query("SELECT COUNT(c) FROM Case c WHERE (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '') AND (c.isArchived IS NULL OR c.isArchived = false) AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND c.caseStatus IN :statuses AND c.createdAt BETWEEN :start AND :end")
    long countLatestCasesByCaseStatusInAndCreatedAtBetween(@Param("statuses") Collection<CaseStatus> statuses, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT c FROM Case c WHERE c.caseStatus IN :statuses AND (c.isArchived IS NULL OR c.isArchived = false) AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)")
    List<Case> findActiveCasesForExpirationCheck(@Param("statuses") Collection<CaseStatus> statuses);



    /**
     * Aggregates total non-deleted root cases grouped by Year and Month within a date range.
     * Includes non-archived, non-deleted, non-permanently-deleted records.
     *
     * @param startDate the inclusive starting timestamp
     * @param endDate the inclusive ending timestamp
     * @param departmentId optional department filter; if {@code null}, aggregates across all departments
     * @return list of monthly count projections ordered chronologically
     */
    @Query("""
             SELECT
                       EXTRACT(YEAR FROM c.createdAt) AS year,
                       EXTRACT(MONTH FROM c.createdAt) AS month,
                       COUNT(c.id) AS count
                   FROM Case c
                             WHERE (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
                             AND (c.isArchived IS NULL OR c.isArchived = false)
                             AND (c.isDeleted IS NULL OR c.isDeleted = false)
                             AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
                             AND (:departmentId IS NULL OR c.department IS NULL OR c.department.id = :departmentId)
                             AND c.createdAt >= :startDate
                             AND c.createdAt <= :endDate
                             GROUP BY EXTRACT(YEAR FROM c.createdAt), EXTRACT(MONTH FROM c.createdAt)
                             ORDER BY EXTRACT(YEAR FROM c.createdAt) ASC, EXTRACT(MONTH FROM c.createdAt)
          """)
    List<CaseCountByMonthProjection> getMonthlyCaseCount(
           @Param("startDate") LocalDateTime startDate,
           @Param("endDate") LocalDateTime endDate,
           @Param("departmentId") Long departmentId
    );

    /**
     * Aggregates total non-deleted root cases grouped by individual calendar day within a date range.
     * Includes non-archived, non-deleted, non-permanently-deleted records.
     *
     * @param startDate the inclusive starting timestamp
     * @param endDate the inclusive ending timestamp
     * @param departmentId optional department filter; if {@code null}, aggregates across all departments
     * @return list of daily count projections ordered chronologically
     */
    @Query("""
        SELECT
            CAST(c.createdAt AS LocalDate) AS date,
            COUNT(c.id) AS count
        FROM Case c
        WHERE (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND (:departmentId IS NULL OR c.department IS NULL OR c.department.id = :departmentId)
          AND c.createdAt >= :startDate
          AND c.createdAt <= :endDate
        GROUP BY CAST(c.createdAt AS LocalDate)
        ORDER BY CAST(c.createdAt AS LocalDate) ASC
    """)
    List<CaseCountByDayProjection> getDailyCaseCounts(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("departmentId") Long departmentId
    );



    /**
     * Aggregates total cases, formal complaints, and settled cases within a given time range and department.
     *
     * @param startDate     the inclusive starting timestamp
     * @param endDate       the inclusive ending timestamp
     * @param departmentId  the target department ID (optional)
     * @param formalType    the enum value for formal complaint filing type
     * @param settledStatus the enum value for settled case status
     * @return projection containing the aggregated counts
     */
    @Query("""
        SELECT
            SUM(CASE WHEN (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') THEN 1L ELSE 0L END) AS totalCases,
            SUM(CASE WHEN (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') AND (c.filingType = :formalType OR c.filingType IS NULL) THEN 1L ELSE 0L END) AS totalFormalComplaints,
            SUM(CASE WHEN (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '') AND c.caseStatus = :settledStatus THEN 1L ELSE 0L END) AS totalSettledCases
        FROM Case c
        WHERE (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND (:departmentId IS NULL OR c.department IS NULL OR c.department.id = :departmentId)
          AND c.createdAt >= :startDate
          AND c.createdAt <= :endDate
    """)
    SettlementMetricsProjection getSettlementEfficiencyMetrics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("departmentId") Long departmentId,
            @Param("formalType") FilingType formalType,
            @Param("settledStatus") CaseStatus settledStatus
    );


    /**
     * Aggregates case counts grouped by case status for latest active cycles within a given time range and department.
     *
     * @param startDate    the inclusive starting timestamp
     * @param endDate      the inclusive ending timestamp
     * @param departmentId optional target department ID
     * @return list of status projections containing counts per status
     */
    @Query("""
        SELECT
            c.caseStatus AS status,
            COUNT(c.id) AS count
        FROM Case c
        WHERE (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND (:departmentId IS NULL OR c.department IS NULL OR c.department.id = :departmentId)
          AND c.createdAt >= :startDate
          AND c.createdAt <= :endDate
          AND c.caseStatus IS NOT NULL
        GROUP BY c.caseStatus
    """)
    List<CaseStatusCountProjection> getCaseStatusCounts(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("departmentId") Long departmentId
    );


    /**
     * Aggregates resolution metrics for blotter cases (latest active cycle / endmost child) within a date range.
     *
     * @param startDate     inclusive starting timestamp
     * @param endDate       inclusive ending timestamp
     * @param departmentId  optional department filter
     * @param formalType    enum constant for FORMAL_COMPLAINT
     * @param settledStatus enum constant for SETTLED status
     * @return {@link FormalComplaintResolutionProjection} containing aggregated counts
     */
    @Query("""
        SELECT
            COUNT(c.id) AS totalFormalComplaints,
            SUM(CASE WHEN c.luponReferral IS NULL AND c.caseStatus = :settledStatus THEN 1L ELSE 0L END) AS settledAtBlotter,
            SUM(CASE WHEN c.luponReferral IS NOT NULL THEN 1L ELSE 0L END) AS referredToLupon,
            SUM(CASE WHEN c.luponReferral IS NULL AND c.caseStatus != :settledStatus THEN 1L ELSE 0L END) AS pendingAtBlotter
        FROM Case c
        WHERE (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND (:departmentId IS NULL OR c.department IS NULL OR c.department.id = :departmentId)
          AND c.createdAt >= :startDate
          AND c.createdAt <= :endDate
    """)
    FormalComplaintResolutionProjection getFormalComplaintResolutionMetrics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("departmentId") Long departmentId,
            @Param("formalType") FilingType formalType,
            @Param("settledStatus") CaseStatus settledStatus
    );

    /**
     * Aggregates KPI metrics (total root cases, settled cases, active mediations, lupon referrals) for Blotter reports.
     * Excludes soft-deleted, permanently deleted, and archived records.
     */
    @Query("""
        SELECT
            SUM(CASE WHEN (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') THEN 1L ELSE 0L END) AS totalCases,
            SUM(CASE WHEN (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '') AND c.caseStatus = :settledStatus THEN 1L ELSE 0L END) AS settledCases,
            SUM(CASE WHEN (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '') AND c.caseStatus IN :activeStatuses THEN 1L ELSE 0L END) AS activeMediations,
            SUM(CASE WHEN (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '') AND c.luponReferral IS NOT NULL THEN 1L ELSE 0L END) AS luponReferrals
        FROM Case c
        WHERE (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND (:departmentId IS NULL OR c.department IS NULL OR c.department.id = :departmentId)
          AND c.createdAt >= :startDate
          AND c.createdAt <= :endDate
    """)
    BlotterReportKpiProjection getBlotterReportKpiMetrics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("departmentId") Long departmentId,
            @Param("settledStatus") CaseStatus settledStatus,
            @Param("activeStatuses") Collection<CaseStatus> activeStatuses
    );


    /**
     * Aggregates lifecycle counts for Blotter records (Archived and Recently Deleted).
     *
     * @param startOfMonth start timestamp of the current month
     * @param endOfMonth   end timestamp of the current month
     * @return {@link BlotterLifecycleKpiProjection} containing aggregated metrics
     */
    @Query("""
        SELECT
            SUM(CASE WHEN c.isArchived = true AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') THEN 1L ELSE 0L END) AS totalArchived,
            SUM(CASE WHEN c.isArchived = true AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') 
                     AND c.updatedAt >= :startOfMonth AND c.updatedAt <= :endOfMonth THEN 1L ELSE 0L END) AS totalArchivedThisMonth,
            SUM(CASE WHEN c.isDeleted = true AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') THEN 1L ELSE 0L END) AS totalRecentlyDeleted,
            SUM(CASE WHEN c.isDeleted = true AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') 
                     AND c.deletedAt >= :startOfMonth AND c.deletedAt <= :endOfMonth THEN 1L ELSE 0L END) AS totalRecentlyDeletedThisMonth
        FROM Case c
        WHERE (c.department IS NULL OR UPPER(c.department.name) = 'BLOTTER')
    """)
    BlotterLifecycleKpiProjection getBlotterLifecycleKpiMetrics(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth
    );
}
