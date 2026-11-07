package com.barangay.cases.bcpc.repository;

import com.barangay.cases.bcpc.model.BcpcReferral;
import com.barangay.cases.shared.model.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BcpcReferralRepository extends JpaRepository<BcpcReferral, Long> {

    List<BcpcReferral> findByCases(Case aCase);

    List<BcpcReferral> findByCasesId(Long caseId);

    List<BcpcReferral> findByCasesIdOrderByReferralDateDesc(Long caseId);

    long countByCasesId(Long caseId);

    @Query("SELECT COUNT(DISTINCT r.cases.id) FROM BcpcReferral r WHERE r.status != 'Cancelled'")
    long countDistinctReferredCases();

    Optional<BcpcReferral> findByReferralControlNumber(String referralControlNumber);

    boolean existsByReferralControlNumber(String referralControlNumber);
}
