package com.barangay.cases.shared.repository;

import com.barangay.cases.shared.model.CaseEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CaseEvidenceRepository extends JpaRepository<CaseEvidence, Long> {
    List<CaseEvidence> findByCasesId(Long caseId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CaseEvidence ce WHERE ce.cases.id = :caseId")
    void deleteByCasesId(@Param("caseId") Long caseId);
}
