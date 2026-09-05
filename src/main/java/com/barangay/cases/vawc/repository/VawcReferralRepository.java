package com.barangay.cases.vawc.repository;

import com.barangay.cases.vawc.model.VawcReferral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VawcReferralRepository extends JpaRepository<VawcReferral, Long> {
    List<VawcReferral> findByCasesId(Long caseId);
    Optional<VawcReferral> findByReferralControlNumber(String referralControlNumber);
    boolean existsByReferralControlNumber(String referralControlNumber);
}
