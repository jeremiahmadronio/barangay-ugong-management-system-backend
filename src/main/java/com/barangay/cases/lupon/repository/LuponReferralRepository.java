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

    /**
     * Retrieves a Lupon referral record by its associated case database ID.
     *
     * @param caseId primary key of the associated case
     * @return an {@link Optional} containing the referral if found, otherwise empty
     */
    Optional<LuponReferral> findByCasesId(Long caseId);

    /**
     * Retrieves a Lupon referral record by the unique case tracking number.
     *
     * @param caseNumber official case number (e.g., "BLOT-2026-0001")
     * @return an {@link Optional} containing the referral if found, otherwise empty
     */
    Optional<LuponReferral> findByCasesCaseNumber(String caseNumber);
}
