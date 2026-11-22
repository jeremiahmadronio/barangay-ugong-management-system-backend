package com.barangay.cases.lupon.repository;

import com.barangay.cases.lupon.model.LuponReferral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for managing {@link LuponReferral} persistence and lookup operations.
 */
@Repository
public interface LuponReferralRepository extends JpaRepository<LuponReferral, Long> {

    Optional<LuponReferral> findByCasesId(Long caseId);
}
