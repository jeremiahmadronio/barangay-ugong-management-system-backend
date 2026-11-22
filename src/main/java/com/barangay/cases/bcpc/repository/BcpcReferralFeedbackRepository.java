package com.barangay.cases.bcpc.repository;

import com.barangay.cases.bcpc.model.BcpcReferralFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing BCPC inter-agency referral feedbacks.
 */
@Repository
public interface BcpcReferralFeedbackRepository extends JpaRepository<BcpcReferralFeedback, Long> {
    List<BcpcReferralFeedback> findByReferralIdOrderByCreatedAtDesc(Long referralId);
}

