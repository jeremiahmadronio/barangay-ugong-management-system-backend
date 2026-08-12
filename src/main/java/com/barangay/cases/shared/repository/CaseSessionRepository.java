package com.barangay.cases.shared.repository;

import com.barangay.cases.shared.model.CaseSession;
import com.barangay.zshared.enumerated.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseSessionRepository extends JpaRepository<CaseSession, Long> {

    /**
     * Retrieves all sessions of a given type for a specific case,
     * ordered by hearing (summon) number ascending.
     */
    List<CaseSession> findByCasesIdAndHearingTypeOrderBySummonNumberAsc(
            Long caseId, SessionType hearingType);
}
