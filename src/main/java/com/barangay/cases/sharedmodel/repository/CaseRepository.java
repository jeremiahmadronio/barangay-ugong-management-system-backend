package com.barangay.cases.sharedmodel.repository;

import com.barangay.cases.sharedmodel.Case;
import com.barangay.cases.sharedmodel.custom.CaseRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Access point for Case data combining Spring Data JPA with CaseRepositoryCustom.
 */
@Repository
public interface CaseRepository extends JpaRepository<Case, Long>, CaseRepositoryCustom {
}
