package com.barangay.cases.sharedmodel.repository;

import com.barangay.cases.sharedmodel.CaseEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaseEvidenceRepository extends JpaRepository<CaseEvidence, Long> {
}
