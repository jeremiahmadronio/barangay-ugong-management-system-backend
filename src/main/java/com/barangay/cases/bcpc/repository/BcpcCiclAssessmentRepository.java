package com.barangay.cases.bcpc.repository;

import com.barangay.cases.bcpc.model.BcpcCaseDetails;
import com.barangay.cases.bcpc.model.BcpcCiclAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for managing Child in Conflict with the Law (CICL) assessment entities.
 */
@Repository
public interface BcpcCiclAssessmentRepository extends JpaRepository<BcpcCiclAssessment, Long> {

    Optional<BcpcCiclAssessment> findByBcpcCaseDetails(BcpcCaseDetails bcpcCaseDetails);

}

