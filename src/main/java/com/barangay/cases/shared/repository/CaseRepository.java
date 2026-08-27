package com.barangay.cases.shared.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import com.barangay.cases.shared.projection.CaseCountByDayProjection;
import com.barangay.cases.shared.projection.CaseCountByMonthProjection;
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

    // KPI Count queries for current month vs previous month trends
    long countByCreatedAtGreaterThanEqual(LocalDateTime start);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByFilingType(FilingType filingType);

    long countByFilingTypeAndCreatedAtGreaterThanEqual(FilingType filingType, LocalDateTime start);

    long countByFilingTypeAndCreatedAtBetween(FilingType filingType, LocalDateTime start, LocalDateTime end);

    long countByCaseStatusIn(Collection<CaseStatus> statuses);

    long countByCaseStatusInAndCreatedAtGreaterThanEqual(Collection<CaseStatus> statuses, LocalDateTime start);

    long countByCaseStatusInAndCreatedAtBetween(Collection<CaseStatus> statuses, LocalDateTime start, LocalDateTime end);

    @Query("SELECT c FROM Case c WHERE c.caseStatus IN :statuses AND (c.isArchived IS NULL OR c.isArchived = false)")
    List<Case> findActiveCasesForExpirationCheck(@Param("statuses") Collection<CaseStatus> statuses);



    /**
     * Aggregates total non-deleted cases grouped by Year and Month within a date range.
     * Includes archived historical records while filtering out soft-deleted records.
     *
     * @param startDate the inclusive starting timestamp
     * @param endDate the inclusive ending timestamp
     * @param departmentId optional department filter; if {@code null}, aggregates across all departments
     * @return list of monthly count projections ordered chronologically
     */
    @Query("""
             SELECT
                       EXTRACT (YEAR FROM c.createdAt) AS YEAR,
                       EXTRACT(MONTH FROM c.createdAt) AS MONTH,
                       COUNT(c.id) AS COUNT
                  FROM Case c
                            WHERE c.isArchived = false
                            AND (:departmentId IS NULL OR c.department.id = :departmentId)
                            AND c.createdAt >= :startDate
                            AND c.createdAt >= :endDate
                            GROUP BY EXTRACT(YEAR FROM c.createdAt), EXTRACT(MONTH FROM c.createdAt)
                            ORDER BY EXTRACT(YEAR FROM c.createdAt) ASC, EXTRACT(MONTH FROM c.createdAt)
          """)
    List<CaseCountByMonthProjection> getMonthlyCaseCount(
           @Param("startDate") LocalDateTime startDate,
           @Param("endDate") LocalDateTime endDate,
           @Param("departmentId") Long departmentId
    );

    /**
     * Aggregates total non-deleted cases grouped by individual calendar day within a date range.
     * Includes archived historical records while filtering out soft-deleted records.
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
        WHERE c.isArchived = false
          AND (:departmentId IS NULL OR c.department.id = :departmentId)
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

}
