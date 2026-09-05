package com.barangay.cases.vawc.repository;

import com.barangay.cases.vawc.model.ReferralFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReferralFeedbackRepository extends JpaRepository<ReferralFeedback, Long> {
    Optional<ReferralFeedback> findByVawcReferralId(Long referralId);
}
