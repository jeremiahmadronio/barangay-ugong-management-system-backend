package com.barangay.cases.shared.repository;

import com.barangay.cases.shared.model.CaseEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseEvidenceRepository extends JpaRepository<CaseEvidence, Long> {
    List<CaseEvidence> findByCasesId(Long caseId);
}

