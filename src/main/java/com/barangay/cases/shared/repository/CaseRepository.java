package com.barangay.cases.shared.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import com.barangay.cases.blotter.dto.RecentBlotterCaseResponse;
import com.barangay.cases.shared.projection.*;
import org.springframework.data.domain.Pageable;
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
     * Efficient JPQL query to retrieve all active cases where a person is involved as Complainant, Respondent, or Witness.
     * Matches by person ID or by normalized first name and last name.
     * Excludes soft-deleted, permanently-deleted, and archived cases.
     */
    @Query("""
        SELECT DISTINCT c FROM Case c
        LEFT JOIN c.complainant comp
        LEFT JOIN comp.person compP
        LEFT JOIN c.respondent resp
        LEFT JOIN resp.person respP
        LEFT JOIN c.witnesses w
        LEFT JOIN w.person wP
        WHERE (
            (compP.id = :personId OR (:firstName IS NOT NULL AND :lastName IS NOT NULL AND LOWER(TRIM(compP.firstName)) = LOWER(TRIM(:firstName)) AND LOWER(TRIM(compP.lastName)) = LOWER(TRIM(:lastName))))
            OR
            (respP.id = :personId OR (:firstName IS NOT NULL AND :lastName IS NOT NULL AND LOWER(TRIM(respP.firstName)) = LOWER(TRIM(:firstName)) AND LOWER(TRIM(respP.lastName)) = LOWER(TRIM(:lastName))))
            OR
            (wP.id = :personId OR (:firstName IS NOT NULL AND :lastName IS NOT NULL AND LOWER(TRIM(wP.firstName)) = LOWER(TRIM(:firstName)) AND LOWER(TRIM(wP.lastName)) = LOWER(TRIM(:lastName))))
        )
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND (c.isArchived IS NULL OR c.isArchived = false)
        ORDER BY c.createdAt DESC
    """)
    List<Case> findByPersonInCaseRoles(
            @Param("personId") Long personId,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName
    );

    @Query("""
        SELECT DISTINCT c FROM Case c
        LEFT JOIN c.complainant comp
        LEFT JOIN comp.person compP
        LEFT JOIN c.respondent resp
        LEFT JOIN resp.person respP
        LEFT JOIN c.witnesses w
        LEFT JOIN w.person wP
        WHERE (
            (compP.id = :personId OR (:firstName IS NOT NULL AND :lastName IS NOT NULL AND LOWER(TRIM(compP.firstName)) = LOWER(TRIM(:firstName)) AND LOWER(TRIM(compP.lastName)) = LOWER(TRIM(:lastName))))
            OR
            (respP.id = :personId OR (:firstName IS NOT NULL AND :lastName IS NOT NULL AND LOWER(TRIM(respP.firstName)) = LOWER(TRIM(:firstName)) AND LOWER(TRIM(respP.lastName)) = LOWER(TRIM(:lastName))))
            OR
            (wP.id = :personId OR (:firstName IS NOT NULL AND :lastName IS NOT NULL AND LOWER(TRIM(wP.firstName)) = LOWER(TRIM(:firstName)) AND LOWER(TRIM(wP.lastName)) = LOWER(TRIM(:lastName))))
        )
        ORDER BY c.createdAt DESC
    """)
    List<Case> findAllHistoricalCasesByPersonInCaseRoles(
            @Param("personId") Long personId,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName
    );

    @Query("""
        SELECT DISTINCT c FROM Case c
        LEFT JOIN c.officer off
        LEFT JOIN off.person offP
        LEFT JOIN c.caseFilledBy user
        LEFT JOIN user.person userP
        WHERE off.id = :officerId
           OR (offP.id IS NOT NULL AND offP.id = :personId)
           OR (userP.id IS NOT NULL AND userP.id = :personId)
        ORDER BY c.createdAt DESC
    """)
    List<Case> findAllCasesHandledByOfficer(
            @Param("officerId") Long officerId,
            @Param("personId") Long personId
    );

    default List<Case> findByPersonIdInCaseRoles(Long personId) {
        return findByPersonInCaseRoles(personId, null, null);
    }

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

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (d IS NULL OR UPPER(d.name) IN ('BLOTTER', 'LUPON'))
          AND (d IS NULL OR UPPER(d.name) != 'VAWC')
          AND UPPER(c.caseNumber) NOT LIKE '%VAWC%'
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
    """)
    long countRootBlotterCases();

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (d IS NULL OR UPPER(d.name) IN ('BLOTTER', 'LUPON'))
          AND (d IS NULL OR UPPER(d.name) != 'VAWC')
          AND UPPER(c.caseNumber) NOT LIKE '%VAWC%'
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.createdAt >= :start
    """)
    long countRootBlotterCasesByCreatedAtGreaterThanEqual(@Param("start") LocalDateTime start);

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (d IS NULL OR UPPER(d.name) IN ('BLOTTER', 'LUPON'))
          AND (d IS NULL OR UPPER(d.name) != 'VAWC')
          AND UPPER(c.caseNumber) NOT LIKE '%VAWC%'
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.createdAt BETWEEN :start AND :end
    """)
    long countRootBlotterCasesByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (d IS NULL OR UPPER(d.name) IN ('BLOTTER', 'LUPON'))
          AND (d IS NULL OR UPPER(d.name) != 'VAWC')
          AND UPPER(c.caseNumber) NOT LIKE '%VAWC%'
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.filingType = :filingType
    """)
    long countRootBlotterCasesByFilingType(@Param("filingType") FilingType filingType);

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (d IS NULL OR UPPER(d.name) IN ('BLOTTER', 'LUPON'))
          AND (d IS NULL OR UPPER(d.name) != 'VAWC')
          AND UPPER(c.caseNumber) NOT LIKE '%VAWC%'
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.filingType = :filingType
          AND c.createdAt >= :start
    """)
    long countRootBlotterCasesByFilingTypeAndCreatedAtGreaterThanEqual(
            @Param("filingType") FilingType filingType,
            @Param("start") LocalDateTime start
    );

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (d IS NULL OR UPPER(d.name) IN ('BLOTTER', 'LUPON'))
          AND (d IS NULL OR UPPER(d.name) != 'VAWC')
          AND UPPER(c.caseNumber) NOT LIKE '%VAWC%'
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.filingType = :filingType
          AND c.createdAt BETWEEN :start AND :end
    """)
    long countRootBlotterCasesByFilingTypeAndCreatedAtBetween(
            @Param("filingType") FilingType filingType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (d IS NULL OR UPPER(d.name) IN ('BLOTTER', 'LUPON'))
          AND (d IS NULL OR UPPER(d.name) != 'VAWC')
          AND UPPER(c.caseNumber) NOT LIKE '%VAWC%'
          AND (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.caseStatus IN :statuses
    """)
    long countLatestBlotterCasesByCaseStatusIn(@Param("statuses") Collection<CaseStatus> statuses);

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (d IS NULL OR UPPER(d.name) IN ('BLOTTER', 'LUPON'))
          AND (d IS NULL OR UPPER(d.name) != 'VAWC')
          AND UPPER(c.caseNumber) NOT LIKE '%VAWC%'
          AND (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.caseStatus IN :statuses
    """)
    long countLatestSettledBlotterCases(@Param("statuses") Collection<CaseStatus> statuses);

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (d IS NULL OR UPPER(d.name) IN ('BLOTTER', 'LUPON'))
          AND (d IS NULL OR UPPER(d.name) != 'VAWC')
          AND UPPER(c.caseNumber) NOT LIKE '%VAWC%'
          AND (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.caseStatus IN :statuses
          AND c.settledAt >= :start
    """)
    long countLatestSettledBlotterCasesBySettledAtGreaterThanEqual(
            @Param("statuses") Collection<CaseStatus> statuses,
            @Param("start") LocalDateTime start
    );

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (d IS NULL OR UPPER(d.name) IN ('BLOTTER', 'LUPON'))
          AND (d IS NULL OR UPPER(d.name) != 'VAWC')
          AND UPPER(c.caseNumber) NOT LIKE '%VAWC%'
          AND (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.caseStatus IN :statuses
          AND c.settledAt BETWEEN :start AND :end
    """)
    long countLatestSettledBlotterCasesBySettledAtBetween(
            @Param("statuses") Collection<CaseStatus> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // =========================================================================
    // BCPC KPI Statistics Queries
    // =========================================================================

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (
            (d IS NOT NULL AND UPPER(d.name) = 'BCPC')
            OR c.bcpcCaseDetails IS NOT NULL
            OR UPPER(c.caseNumber) LIKE '%BCPC%'
        )
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
    """)
    long countRootBcpcCases();

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (
            (d IS NOT NULL AND UPPER(d.name) = 'BCPC')
            OR c.bcpcCaseDetails IS NOT NULL
            OR UPPER(c.caseNumber) LIKE '%BCPC%'
        )
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.createdAt >= :start
    """)
    long countRootBcpcCasesByCreatedAtGreaterThanEqual(@Param("start") LocalDateTime start);

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (
            (d IS NOT NULL AND UPPER(d.name) = 'BCPC')
            OR c.bcpcCaseDetails IS NOT NULL
            OR UPPER(c.caseNumber) LIKE '%BCPC%'
        )
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.createdAt BETWEEN :start AND :end
    """)
    long countRootBcpcCasesByCreatedAtBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (
            (d IS NOT NULL AND UPPER(d.name) = 'BCPC')
            OR c.bcpcCaseDetails IS NOT NULL
            OR UPPER(c.caseNumber) LIKE '%BCPC%'
        )
          AND (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.caseStatus IN :statuses
    """)
    long countLatestBcpcCasesByCaseStatusIn(@Param("statuses") Collection<CaseStatus> statuses);


    @Query("SELECT c FROM Case c WHERE c.caseStatus IN :statuses AND (c.isArchived IS NULL OR c.isArchived = false) AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)")
    List<Case> findActiveCasesForExpirationCheck(@Param("statuses") Collection<CaseStatus> statuses);

    @Query("""
        SELECT c FROM Case c
        LEFT JOIN c.department d
        WHERE (
            (d IS NOT NULL AND UPPER(d.name) = 'BCPC')
            OR c.bcpcCaseDetails IS NOT NULL
            OR UPPER(c.caseNumber) LIKE '%BCPC%'
        )
          AND c.caseStatus IN :statuses
          AND c.createdAt <= :cutoff
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
    """)
    List<Case> findPendingBcpcCasesForExpirationCheck(
            @Param("statuses") Collection<CaseStatus> statuses,
            @Param("cutoff") LocalDateTime cutoff
    );

    @Query("SELECT c FROM Case c WHERE c.isDeleted = true AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND c.deletedAt IS NOT NULL AND c.deletedAt <= :cutoff")
    List<Case> findExpiredSoftDeletedCases(@Param("cutoff") LocalDateTime cutoff);



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
                             AND (:departmentId IS NULL OR c.department IS NULL OR UPPER(c.department.name) IN ('BLOTTER', 'LUPON'))
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

    // ==========================================
    // VAWC KPI STATISTICAL QUERIES
    // ==========================================

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (UPPER(d.name) = 'VAWC' OR (d IS NULL AND UPPER(c.caseNumber) LIKE '%VAWC%'))
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
    """)
    long countRootVawcCases();

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (UPPER(d.name) = 'VAWC' OR (d IS NULL AND UPPER(c.caseNumber) LIKE '%VAWC%'))
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.createdAt >= :start
    """)
    long countRootVawcCasesByCreatedAtGreaterThanEqual(@Param("start") LocalDateTime start);

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (UPPER(d.name) = 'VAWC' OR (d IS NULL AND UPPER(c.caseNumber) LIKE '%VAWC%'))
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.createdAt BETWEEN :start AND :end
    """)
    long countRootVawcCasesByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (UPPER(d.name) = 'VAWC' OR (d IS NULL AND UPPER(c.caseNumber) LIKE '%VAWC%'))
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.caseStatus IN :statuses
    """)
    long countVawcCasesByCaseStatusIn(@Param("statuses") Collection<CaseStatus> statuses);

    @Query("""
        SELECT c FROM Case c
        LEFT JOIN c.department d
        WHERE (UPPER(d.name) = 'VAWC' OR (d IS NULL AND UPPER(c.caseNumber) LIKE '%VAWC%'))
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
        ORDER BY c.createdAt DESC
    """)
    List<Case> findRootVawcCases();

    @Query("""
        SELECT c FROM Case c
        LEFT JOIN c.department d
        WHERE (UPPER(d.name) = 'VAWC' OR (d IS NULL AND UPPER(c.caseNumber) LIKE '%VAWC%'))
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.createdAt BETWEEN :start AND :end
        ORDER BY c.createdAt DESC
    """)
    List<Case> findRootVawcCasesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        SELECT COUNT(c) FROM Case c
        LEFT JOIN c.department d
        WHERE (UPPER(d.name) = 'VAWC' OR (d IS NULL AND UPPER(c.caseNumber) LIKE '%VAWC%'))
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.caseStatus IN :statuses
          AND c.createdAt BETWEEN :start AND :end
    """)
    long countVawcCasesByCaseStatusInBetween(
            @Param("statuses") Collection<CaseStatus> statuses,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("""
        SELECT
            EXTRACT(YEAR FROM c.createdAt) AS year,
            EXTRACT(MONTH FROM c.createdAt) AS month,
            COUNT(c.id) AS count
        FROM Case c
        LEFT JOIN c.department d
        WHERE (UPPER(d.name) = 'VAWC' OR (d IS NULL AND UPPER(c.caseNumber) LIKE '%VAWC%'))
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.createdAt >= :startDate
          AND c.createdAt <= :endDate
        GROUP BY EXTRACT(YEAR FROM c.createdAt), EXTRACT(MONTH FROM c.createdAt)
        ORDER BY EXTRACT(YEAR FROM c.createdAt) ASC, EXTRACT(MONTH FROM c.createdAt) ASC
    """)
    List<CaseCountByMonthProjection> getVawcMonthlyCaseCounts(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT
            CAST(c.createdAt AS LocalDate) AS date,
            COUNT(c.id) AS count
        FROM Case c
        LEFT JOIN c.department d
        WHERE (UPPER(d.name) = 'VAWC' OR (d IS NULL AND UPPER(c.caseNumber) LIKE '%VAWC%'))
          AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.createdAt >= :startDate
          AND c.createdAt <= :endDate
        GROUP BY CAST(c.createdAt AS LocalDate)
        ORDER BY CAST(c.createdAt AS LocalDate) ASC
    """)
    List<CaseCountByDayProjection> getVawcDailyCaseCounts(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
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
          AND (:departmentId IS NULL OR c.department IS NULL OR UPPER(c.department.name) IN ('BLOTTER', 'LUPON'))
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
          AND (:departmentId IS NULL OR c.department IS NULL OR UPPER(c.department.name) IN ('BLOTTER', 'LUPON'))
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
          AND (:departmentId IS NULL OR c.department IS NULL OR UPPER(c.department.name) IN ('BLOTTER', 'LUPON'))
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
            SUM(CASE WHEN c.luponReferral IS NOT NULL OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON') THEN 1L ELSE 0L END) AS referredToLupon,
            SUM(CASE WHEN c.luponReferral IS NULL AND c.caseStatus != :settledStatus AND c.caseStatus != com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION AND (c.department IS NULL OR UPPER(c.department.name) != 'LUPON') THEN 1L ELSE 0L END) AS pendingAtBlotter
        FROM Case c
        WHERE (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND (:departmentId IS NULL OR c.department IS NULL OR UPPER(c.department.name) IN ('BLOTTER', 'LUPON'))
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
            SUM(CASE WHEN (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '') AND (c.luponReferral IS NOT NULL OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON')) THEN 1L ELSE 0L END) AS luponReferrals
        FROM Case c
        WHERE (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND (:departmentId IS NULL OR c.department IS NULL OR UPPER(c.department.name) IN ('BLOTTER', 'LUPON'))
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
                     AND ((c.archivedAt IS NOT NULL AND c.archivedAt >= :startOfMonth AND c.archivedAt <= :endOfMonth) OR (c.archivedAt IS NULL AND c.updatedAt >= :startOfMonth AND c.updatedAt <= :endOfMonth)) THEN 1L ELSE 0L END) AS totalArchivedThisMonth,
            SUM(CASE WHEN c.isDeleted = true AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') THEN 1L ELSE 0L END) AS totalRecentlyDeleted,
            SUM(CASE WHEN (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
                     AND ((c.isDeleted = true AND c.deletedAt IS NOT NULL AND c.deletedAt >= :startOfMonth AND c.deletedAt <= :endOfMonth)
                          OR (c.isPermanentDeleted = true AND c.permanentDeletedAt IS NOT NULL AND c.permanentDeletedAt >= :startOfMonth AND c.permanentDeletedAt <= :endOfMonth)
                          OR ((c.isDeleted = true OR c.isPermanentDeleted = true) AND c.updatedAt >= :startOfMonth AND c.updatedAt <= :endOfMonth)) THEN 1L ELSE 0L END) AS totalRecentlyDeletedThisMonth
        FROM Case c
        WHERE (c.department IS NULL OR UPPER(c.department.name) IN ('BLOTTER', 'LUPON'))
    """)
    BlotterLifecycleKpiProjection getBlotterLifecycleKpiMetrics(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth
    );


    /**
     * Aggregates Blotter Case KPIs:
     * 1. New root cases filed this week (Monday to Sunday)
     * 2. Active pending endmost cases
     * 3. Unresolved blotter cases (Pending + Under Mediation, strictly excluding Conciliation/Settled)
     */
    @Query("""
        SELECT
            SUM(CASE WHEN (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
                     AND c.createdAt >= :startOfWeek AND c.createdAt <= :endOfWeek THEN 1L ELSE 0L END) AS newCasesThisWeek,
            SUM(CASE WHEN (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '')
                     AND c.caseStatus = :pendingStatus THEN 1L ELSE 0L END) AS pendingCases,
            SUM(CASE WHEN (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '')
                     AND c.caseStatus IN :unresolvedStatuses THEN 1L ELSE 0L END) AS unresolvedCases
        FROM Case c
        WHERE (c.department IS NULL OR UPPER(c.department.name) IN ('BLOTTER', 'LUPON'))
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
    """)
    BlotterDashboardKpiProjection getBlotterDashboardCaseKpis(
            @Param("startOfWeek") LocalDateTime startOfWeek,
            @Param("endOfWeek") LocalDateTime endOfWeek,
            @Param("pendingStatus") CaseStatus pendingStatus,
            @Param("unresolvedStatuses") Collection<CaseStatus> unresolvedStatuses
    );


    /**
     * Retrieves recent blotter cases filed today, ordered from newest to oldest.
     * Excludes archived, soft-deleted, and permanently deleted records.
     *
     * @param startOfDay start of the current day (00:00:00)
     * @param endOfDay   end of the current day (23:59:59.999)
     * @param pageable   pagination limit (e.g. PageRequest.of(0, 5))
     * @return list of {@link RecentBlotterCaseResponse} (max 5)
     */
    @Query("""
        SELECT new com.barangay.cases.blotter.dto.RecentBlotterCaseResponse(
            c.id,
            c.caseNumber,
            TRIM(CONCAT(COALESCE(compPerson.firstName, ''), ' ', COALESCE(compPerson.lastName, ''))),
            TRIM(CONCAT(COALESCE(respPerson.firstName, ''), ' ', COALESCE(respPerson.lastName, ''))),
            c.filingType,
            c.caseStatus,
            c.createdAt
        )
        FROM Case c
        LEFT JOIN c.complainant comp
        LEFT JOIN comp.person compPerson
        LEFT JOIN c.respondent resp
        LEFT JOIN resp.person respPerson
        WHERE (c.department IS NULL OR UPPER(c.department.name) IN ('BLOTTER', 'LUPON'))
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.createdAt >= :startOfDay
          AND c.createdAt <= :endOfDay
        ORDER BY c.createdAt DESC
    """)
    List<RecentBlotterCaseResponse> findRecentBlotterCasesToday(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            Pageable pageable
    );

    @Query("""
        SELECT c
        FROM Case c
        LEFT JOIN FETCH c.complainant comp
        LEFT JOIN FETCH comp.person compPerson
        LEFT JOIN FETCH c.respondent resp
        LEFT JOIN FETCH resp.person respPerson
        WHERE (c.department IS NULL OR UPPER(c.department.name) IN ('BLOTTER', 'LUPON'))
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.createdAt >= :startOfDay
          AND c.createdAt <= :endOfDay
        ORDER BY c.createdAt DESC
    """)
    List<Case> findRecentCasesCreatedToday(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    @Query("""
        SELECT c
        FROM Case c
        LEFT JOIN FETCH c.complainant comp
        LEFT JOIN FETCH comp.person compPerson
        LEFT JOIN FETCH c.respondent resp
        LEFT JOIN FETCH resp.person respPerson
        WHERE (c.luponReferral IS NOT NULL OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON') OR EXISTS (SELECT 1 FROM Case child WHERE child.previousCaseNumber = c.caseNumber AND (child.luponReferral IS NOT NULL OR child.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (child.department IS NOT NULL AND UPPER(child.department.name) = 'LUPON'))))
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND ((c.luponReferral IS NOT NULL AND c.luponReferral.referredAt >= :startOfDay AND c.luponReferral.referredAt <= :endOfDay)
               OR (c.createdAt >= :startOfDay AND c.createdAt <= :endOfDay)
               OR (c.updatedAt >= :startOfDay AND c.updatedAt <= :endOfDay))
        ORDER BY c.createdAt DESC
    """)
    List<Case> findRecentLuponCasesToday(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    /**
     * Counts all distinct root cases referred to Lupon (excluding archived and deleted).
     */
    @Query("""
        SELECT COUNT(c.id)
        FROM Case c
        WHERE (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (
              c.luponReferral IS NOT NULL
              OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION
              OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON')
              OR EXISTS (
                  SELECT 1 FROM Case child
                  WHERE child.previousCaseNumber = c.caseNumber
                    AND (child.luponReferral IS NOT NULL OR child.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (child.department IS NOT NULL AND UPPER(child.department.name) = 'LUPON'))
              )
          )
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
    """)
    long countTotalLuponReferredCases();

    /**
     * Counts all distinct root cases referred to Lupon within a creation date range.
     */
    @Query("""
        SELECT COUNT(c.id)
        FROM Case c
        WHERE (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
          AND (
              c.luponReferral IS NOT NULL
              OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION
              OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON')
              OR EXISTS (
                  SELECT 1 FROM Case child
                  WHERE child.previousCaseNumber = c.caseNumber
                    AND (child.luponReferral IS NOT NULL OR child.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (child.department IS NOT NULL AND UPPER(child.department.name) = 'LUPON'))
              )
          )
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.createdAt >= :startDate
          AND c.createdAt <= :endDate
    """)
    long countTotalLuponReferredCasesBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Counts active cases currently under conciliation in Lupon (latest cycle).
     */
    @Query("""
        SELECT COUNT(c.id)
        FROM Case c
        WHERE (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '')
          AND c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
    """)
    long countActiveLuponConciliationCases();

    /**
     * Counts cases referred to Lupon that were successfully settled.
     */
    @Query("""
        SELECT COUNT(c.id)
        FROM Case c
        WHERE (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '')
          AND (c.luponReferral IS NOT NULL OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON') OR EXISTS (SELECT 1 FROM Case parent WHERE parent.caseNumber = c.previousCaseNumber AND (parent.luponReferral IS NOT NULL OR (parent.department IS NOT NULL AND UPPER(parent.department.name) = 'LUPON'))))
          AND c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.SETTLED
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
    """)
    long countSuccessfullySettledLuponCases();

    /**
     * Counts cases referred to Lupon that resulted in Certificate to File Action (CFA) issuance.
     */
    @Query("""
        SELECT COUNT(c.id)
        FROM Case c
        WHERE (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '')
          AND (c.luponReferral IS NOT NULL OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON') OR c.certificateToFileAction IS NOT NULL OR EXISTS (SELECT 1 FROM Case parent WHERE parent.caseNumber = c.previousCaseNumber AND (parent.luponReferral IS NOT NULL OR (parent.department IS NOT NULL AND UPPER(parent.department.name) = 'LUPON'))))
          AND (c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.CERTIFIED_TO_FILE_ACTION OR c.certificateToFileAction IS NOT NULL)
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
    """)
    long countCfaIssuedLuponCases();

    /**
     * Aggregates KPI metrics (Total Referred, Settled Cases, Active Conciliations, CFA Issued) for Lupon reports.
     */
    @Query("""
        SELECT
            SUM(CASE WHEN (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') AND (c.luponReferral IS NOT NULL OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON') OR EXISTS (SELECT 1 FROM Case child WHERE child.previousCaseNumber = c.caseNumber AND (child.luponReferral IS NOT NULL OR child.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (child.department IS NOT NULL AND UPPER(child.department.name) = 'LUPON')))) THEN 1L ELSE 0L END) AS totalReferred,
            SUM(CASE WHEN (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '') AND c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.SETTLED AND (c.luponReferral IS NOT NULL OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON') OR EXISTS (SELECT 1 FROM Case parent WHERE parent.caseNumber = c.previousCaseNumber AND (parent.luponReferral IS NOT NULL OR (parent.department IS NOT NULL AND UPPER(parent.department.name) = 'LUPON')))) THEN 1L ELSE 0L END) AS settledCases,
            SUM(CASE WHEN (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '') AND c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION THEN 1L ELSE 0L END) AS activeConciliations,
            SUM(CASE WHEN (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '') AND (c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.CERTIFIED_TO_FILE_ACTION OR c.certificateToFileAction IS NOT NULL) AND (c.luponReferral IS NOT NULL OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON') OR c.certificateToFileAction IS NOT NULL OR EXISTS (SELECT 1 FROM Case parent WHERE parent.caseNumber = c.previousCaseNumber AND (parent.luponReferral IS NOT NULL OR (parent.department IS NOT NULL AND UPPER(parent.department.name) = 'LUPON')))) THEN 1L ELSE 0L END) AS cfaIssued
        FROM Case c
        WHERE (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND c.createdAt >= :startDate
          AND c.createdAt <= :endDate
    """)
    com.barangay.cases.shared.projection.LuponReportKpiProjection getLuponReportKpiMetrics(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Aggregates total Lupon-referred root cases grouped by year and month.
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
          AND (c.luponReferral IS NOT NULL OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON') OR EXISTS (SELECT 1 FROM Case child WHERE child.previousCaseNumber = c.caseNumber AND (child.luponReferral IS NOT NULL OR child.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (child.department IS NOT NULL AND UPPER(child.department.name) = 'LUPON'))))
          AND c.createdAt >= :startDate
          AND c.createdAt <= :endDate
        GROUP BY EXTRACT(YEAR FROM c.createdAt), EXTRACT(MONTH FROM c.createdAt)
        ORDER BY EXTRACT(YEAR FROM c.createdAt) ASC, EXTRACT(MONTH FROM c.createdAt) ASC
    """)
    List<CaseCountByMonthProjection> getMonthlyLuponCaseCount(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Aggregates total Lupon-referred root cases grouped by individual calendar day.
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
          AND (c.luponReferral IS NOT NULL OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON') OR EXISTS (SELECT 1 FROM Case child WHERE child.previousCaseNumber = c.caseNumber AND (child.luponReferral IS NOT NULL OR child.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (child.department IS NOT NULL AND UPPER(child.department.name) = 'LUPON'))))
          AND c.createdAt >= :startDate
          AND c.createdAt <= :endDate
        GROUP BY CAST(c.createdAt AS LocalDate)
        ORDER BY CAST(c.createdAt AS LocalDate) ASC
    """)
    List<CaseCountByDayProjection> getDailyLuponCaseCounts(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Aggregates case counts grouped by case status for latest active cycles referred to Lupon.
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
          AND (c.luponReferral IS NOT NULL OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON') OR EXISTS (SELECT 1 FROM Case parent WHERE parent.caseNumber = c.previousCaseNumber AND (parent.luponReferral IS NOT NULL OR (parent.department IS NOT NULL AND UPPER(parent.department.name) = 'LUPON'))))
          AND c.createdAt >= :startDate
          AND c.createdAt <= :endDate
          AND c.caseStatus IS NOT NULL
        GROUP BY c.caseStatus
    """)
    List<CaseStatusCountProjection> getLuponCaseStatusCounts(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * Aggregates Lupon Lifecycle KPIs (Archived and Trash counts for Lupon-referred cases).
     */
    @Query("""
        SELECT
            SUM(CASE WHEN c.isArchived = true AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') THEN 1L ELSE 0L END) AS totalArchived,
            SUM(CASE WHEN c.isArchived = true AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
                     AND ((c.archivedAt IS NOT NULL AND c.archivedAt >= :startOfMonth AND c.archivedAt <= :endOfMonth) OR (c.archivedAt IS NULL AND c.updatedAt >= :startOfMonth AND c.updatedAt <= :endOfMonth)) THEN 1L ELSE 0L END) AS totalArchivedThisMonth,
            SUM(CASE WHEN c.isDeleted = true AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') THEN 1L ELSE 0L END) AS totalRecentlyDeleted,
            SUM(CASE WHEN (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
                     AND ((c.isDeleted = true AND c.deletedAt IS NOT NULL AND c.deletedAt >= :startOfMonth AND c.deletedAt <= :endOfMonth)
                          OR (c.isPermanentDeleted = true AND c.permanentDeletedAt IS NOT NULL AND c.permanentDeletedAt >= :startOfMonth AND c.permanentDeletedAt <= :endOfMonth)
                          OR ((c.isDeleted = true OR c.isPermanentDeleted = true) AND c.updatedAt >= :startOfMonth AND c.updatedAt <= :endOfMonth)) THEN 1L ELSE 0L END) AS totalRecentlyDeletedThisMonth
        FROM Case c
        WHERE (c.luponReferral IS NOT NULL OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON') OR EXISTS (SELECT 1 FROM Case child WHERE child.previousCaseNumber = c.caseNumber AND (child.luponReferral IS NOT NULL OR child.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (child.department IS NOT NULL AND UPPER(child.department.name) = 'LUPON'))))
    """)
    com.barangay.cases.shared.projection.BlotterLifecycleKpiProjection getLuponLifecycleKpiMetrics(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth
    );

    /**
     * Aggregates Lupon Operational Dashboard Case KPIs:
     * 1. Total cases referred to Lupon this week (Monday to Sunday)
     * 2. Active cases currently under conciliation
     * 3. Total Certificate to File Action (CFA) issued this week
     */
    @Query("""
        SELECT
            SUM(CASE WHEN (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
                     AND ((c.luponReferral IS NOT NULL AND c.luponReferral.referredAt >= :startOfWeek AND c.luponReferral.referredAt <= :endOfWeek)
                          OR (c.createdAt >= :startOfWeek AND c.createdAt <= :endOfWeek AND (c.luponReferral IS NOT NULL OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON')))) THEN 1L ELSE 0L END) AS totalReferredThisWeek,
            SUM(CASE WHEN (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '')
                     AND c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION THEN 1L ELSE 0L END) AS totalUnderConciliation,
            SUM(CASE WHEN (c.reOpenedCaseNumber IS NULL OR TRIM(c.reOpenedCaseNumber) = '')
                     AND ((c.certificateToFileAction IS NOT NULL AND c.certificateToFileAction.createdAt >= :startOfWeek AND c.certificateToFileAction.createdAt <= :endOfWeek)
                          OR (c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.CERTIFIED_TO_FILE_ACTION AND c.updatedAt >= :startOfWeek AND c.updatedAt <= :endOfWeek)) THEN 1L ELSE 0L END) AS totalCfaIssuedThisWeek
        FROM Case c
        WHERE (c.luponReferral IS NOT NULL OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON') OR EXISTS (SELECT 1 FROM Case child WHERE child.previousCaseNumber = c.caseNumber AND (child.luponReferral IS NOT NULL OR child.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (child.department IS NOT NULL AND UPPER(child.department.name) = 'LUPON'))))
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
    """)
    com.barangay.cases.lupon.projection.LuponDashboardKpiProjection getLuponDashboardCaseKpis(
            @Param("startOfWeek") LocalDateTime startOfWeek,
            @Param("endOfWeek") LocalDateTime endOfWeek
    );

    /**
     * Aggregates VAWC Lifecycle KPIs (Archived and Trash counts for VAWC cases).
     */
    @Query("""
        SELECT
            SUM(CASE WHEN c.isArchived = true AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') THEN 1L ELSE 0L END) AS totalArchived,
            SUM(CASE WHEN c.isArchived = true AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
                     AND ((c.archivedAt IS NOT NULL AND c.archivedAt >= :startOfMonth AND c.archivedAt <= :endOfMonth) OR (c.archivedAt IS NULL AND c.updatedAt >= :startOfMonth AND c.updatedAt <= :endOfMonth)) THEN 1L ELSE 0L END) AS totalArchivedThisMonth,
            SUM(CASE WHEN c.isDeleted = true AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') THEN 1L ELSE 0L END) AS totalRecentlyDeleted,
            SUM(CASE WHEN (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
                     AND ((c.isDeleted = true AND c.deletedAt IS NOT NULL AND c.deletedAt >= :startOfMonth AND c.deletedAt <= :endOfMonth)
                          OR (c.isPermanentDeleted = true AND c.permanentDeletedAt IS NOT NULL AND c.permanentDeletedAt >= :startOfMonth AND c.permanentDeletedAt <= :endOfMonth)
                          OR ((c.isDeleted = true OR c.isPermanentDeleted = true) AND c.updatedAt >= :startOfMonth AND c.updatedAt <= :endOfMonth)) THEN 1L ELSE 0L END) AS totalRecentlyDeletedThisMonth
        FROM Case c
        WHERE ((c.department IS NOT NULL AND UPPER(c.department.name) = 'VAWC') OR UPPER(c.caseNumber) LIKE '%VAWC%')
    """)
    com.barangay.cases.shared.projection.BlotterLifecycleKpiProjection getVawcLifecycleKpiMetrics(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth
    );

    /**
     * Aggregates BCPC Lifecycle KPIs (Archived and Trash counts for BCPC cases).
     */
    @Query("""
        SELECT
            SUM(CASE WHEN c.isArchived = true AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') THEN 1L ELSE 0L END) AS totalArchived,
            SUM(CASE WHEN c.isArchived = true AND (c.isDeleted IS NULL OR c.isDeleted = false) AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
                     AND ((c.archivedAt IS NOT NULL AND c.archivedAt >= :startOfMonth AND c.archivedAt <= :endOfMonth) OR (c.archivedAt IS NULL AND c.updatedAt >= :startOfMonth AND c.updatedAt <= :endOfMonth)) THEN 1L ELSE 0L END) AS totalArchivedThisMonth,
            SUM(CASE WHEN c.isDeleted = true AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false) AND (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '') THEN 1L ELSE 0L END) AS totalRecentlyDeleted,
            SUM(CASE WHEN (c.previousCaseNumber IS NULL OR TRIM(c.previousCaseNumber) = '')
                     AND ((c.isDeleted = true AND c.deletedAt IS NOT NULL AND c.deletedAt >= :startOfMonth AND c.deletedAt <= :endOfMonth)
                          OR (c.isPermanentDeleted = true AND c.permanentDeletedAt IS NOT NULL AND c.permanentDeletedAt >= :startOfMonth AND c.permanentDeletedAt <= :endOfMonth)
                          OR ((c.isDeleted = true OR c.isPermanentDeleted = true) AND c.updatedAt >= :startOfMonth AND c.updatedAt <= :endOfMonth)) THEN 1L ELSE 0L END) AS totalRecentlyDeletedThisMonth
        FROM Case c
        WHERE ((c.department IS NOT NULL AND UPPER(c.department.name) = 'BCPC') OR UPPER(c.caseNumber) LIKE '%BCPC%')
    """)
    com.barangay.cases.shared.projection.BlotterLifecycleKpiProjection getBcpcLifecycleKpiMetrics(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth
    );

    @Query("SELECT c FROM Case c WHERE c.ftjsApplication.id = :ftjsId " +
           "AND c.relatedBarangayService = :service " +
           "AND c.documentReissuanceStatus IN :statuses " +
           "AND (c.isDeleted IS NULL OR c.isDeleted = false) " +
           "AND (c.isArchived IS NULL OR c.isArchived = false) " +
           "ORDER BY c.createdAt DESC")
    List<Case> findActiveDocumentReissuanceRequests(
            @Param("ftjsId") Long ftjsId,
            @Param("service") com.barangay.zshared.enumerated.RelatedBarangayService service,
            @Param("statuses") List<com.barangay.zshared.enumerated.DocumentReissuanceStatus> statuses
    );

    @Query("SELECT c FROM Case c WHERE c.ftjsApplication.id = :ftjsId " +
           "AND c.relatedBarangayService = :service " +
           "ORDER BY c.createdAt DESC")
    List<Case> findAllByFtjsApplicationIdAndRelatedBarangayServiceOrderByCreatedAtDesc(
            @Param("ftjsId") Long ftjsId,
            @Param("service") com.barangay.zshared.enumerated.RelatedBarangayService service
    );
}
