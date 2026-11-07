package com.barangay.cases.bcpc.repository;

import com.barangay.cases.bcpc.model.BcpcReferralFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BcpcReferralFeedbackRepository extends JpaRepository<BcpcReferralFeedback, Long> {
    List<BcpcReferralFeedback> findByReferralIdOrderByCreatedAtDesc(Long referralId);
    List<BcpcReferralFeedback> findByCasesIdOrderByCreatedAtDesc(Long caseId);
}

