package com.barangay.cases.bcpc.repository;

import com.barangay.cases.bcpc.model.BcpcCaseDetails;
import com.barangay.cases.shared.model.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing BCPC case intake details and child profile information.
 */
@Repository
public interface BcpcCaseDetailsRepository extends JpaRepository<BcpcCaseDetails, Long> {

    Optional<BcpcCaseDetails> findByCases(Case cases);

    Optional<BcpcCaseDetails> findByCasesId(Long caseId);

}
