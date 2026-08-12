package com.barangay.cases.shared.repository;

import com.barangay.cases.shared.model.Case;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Access point for Case data combining Spring Data JPA with CaseRepositoryCustom.
 */
@Repository
public interface CaseRepository extends JpaRepository<Case, Long>, CaseRepositoryCustom {

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
