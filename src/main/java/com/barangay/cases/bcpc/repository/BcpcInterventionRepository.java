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

    List<BcpcIntervention> findByBcpcCaseDetailsIdOrderByCreatedAtDesc(Long bcpcCaseDetailsId);

    long countByBcpcCaseDetailsId(Long bcpcCaseDetailsId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT i FROM BcpcIntervention i
        JOIN FETCH i.bcpcCaseDetails bcd
        JOIN FETCH bcd.cases c
        LEFT JOIN FETCH bcd.child ch
        WHERE UPPER(i.status) = 'ONGOING'
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
        ORDER BY i.createdAt DESC
    """)
    List<BcpcIntervention> findActiveInterventionsForMonitoring();
}

