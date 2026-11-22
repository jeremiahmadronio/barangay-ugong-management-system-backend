package com.barangay.cases.bcpc.repository;

import com.barangay.cases.bcpc.model.BcpcCarAssessment;
import com.barangay.cases.bcpc.model.BcpcCaseDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing Child at Risk (CAR) assessment entities.
 */
@Repository
public interface BcpcCarAssessmentRepository extends JpaRepository<BcpcCarAssessment, Long> {

    Optional<BcpcCarAssessment> findByBcpcCaseDetails(BcpcCaseDetails bcpcCaseDetails);

}

