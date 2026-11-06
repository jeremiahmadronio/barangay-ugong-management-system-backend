package com.barangay.cases.bcpc.repository;

import com.barangay.cases.bcpc.model.BcpcCaseDetails;
import com.barangay.cases.bcpc.model.BcpcIntervention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BcpcInterventionRepository extends JpaRepository<BcpcIntervention, Long> {

    List<BcpcIntervention> findByBcpcCaseDetails(BcpcCaseDetails bcpcCaseDetails);

    List<BcpcIntervention> findByBcpcCaseDetailsId(Long bcpcCaseDetailsId);
}

