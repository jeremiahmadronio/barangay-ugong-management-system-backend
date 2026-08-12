package com.barangay.cases.shared.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

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

    // KPI Count queries for current month vs previous month trends
    long countByCreatedAtGreaterThanEqual(LocalDateTime start);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByFilingType(FilingType filingType);

    long countByFilingTypeAndCreatedAtGreaterThanEqual(FilingType filingType, LocalDateTime start);

    long countByFilingTypeAndCreatedAtBetween(FilingType filingType, LocalDateTime start, LocalDateTime end);

    long countByCaseStatusIn(Collection<CaseStatus> statuses);

    long countByCaseStatusInAndCreatedAtGreaterThanEqual(Collection<CaseStatus> statuses, LocalDateTime start);

    long countByCaseStatusInAndCreatedAtBetween(Collection<CaseStatus> statuses, LocalDateTime start, LocalDateTime end);
}
