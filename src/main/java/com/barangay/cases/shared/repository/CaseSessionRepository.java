package com.barangay.cases.shared.repository;

import com.barangay.cases.shared.model.CaseSession;
import com.barangay.zshared.enumerated.SessionStatus;
import com.barangay.zshared.enumerated.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CaseSessionRepository extends JpaRepository<CaseSession, Long> {

    /**
     * Retrieves all sessions of a given type for a specific case,
     * ordered by hearing (summon) number ascending.
     */
    List<CaseSession> findByCasesIdAndHearingTypeOrderBySummonNumberAsc(
            Long caseId, SessionType hearingType);

    /**
     * Counts scheduled mediation sessions for Blotter cases within a date range (Monday-Sunday).
     * Excludes sessions tied to archived or deleted cases.
     */
    @Query("""
        SELECT COUNT(s.id)
        FROM CaseSession s
        JOIN s.cases c
        WHERE (c.department IS NULL OR UPPER(c.department.name) = 'BLOTTER')
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND s.hearingType = :hearingType
          AND s.status = :sessionStatus
          AND s.scheduledStart >= :startOfWeek
          AND s.scheduledStart <= :endOfWeek
    """)
    long countUpcomingMediationsThisWeek(
            @Param("startOfWeek") LocalDateTime startOfWeek,
            @Param("endOfWeek") LocalDateTime endOfWeek,
            @Param("hearingType") SessionType hearingType,
            @Param("sessionStatus") SessionStatus sessionStatus
    );
}
