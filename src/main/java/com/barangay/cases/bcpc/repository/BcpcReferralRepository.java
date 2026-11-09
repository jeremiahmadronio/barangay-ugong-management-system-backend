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

    @Query("SELECT COUNT(r) FROM BcpcReferral r WHERE r.status != 'Cancelled' AND (r.feedbackResponse IS NULL OR TRIM(r.feedbackResponse) = '')")
    long countPendingAgencyFeedback();

    @Query("""
        SELECT r FROM BcpcReferral r
        JOIN FETCH r.cases c
        LEFT JOIN FETCH c.bcpcCaseDetails bcd
        LEFT JOIN FETCH bcd.child ch
        WHERE r.status != 'Cancelled'
          AND (r.feedbackResponse IS NULL OR TRIM(r.feedbackResponse) = '')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
        ORDER BY r.referralDate ASC
    """)
    List<BcpcReferral> findPendingReferralsForMonitoring();

    Optional<BcpcReferral> findByReferralControlNumber(String referralControlNumber);

    boolean existsByReferralControlNumber(String referralControlNumber);
}
