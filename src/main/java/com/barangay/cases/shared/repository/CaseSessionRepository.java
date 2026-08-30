package com.barangay.cases.shared.repository;

import com.barangay.cases.blotter.dto.QuickMediationSummaryDto;
import com.barangay.cases.shared.model.CaseSession;
import com.barangay.zshared.enumerated.SessionStatus;
import com.barangay.zshared.enumerated.SessionType;
import org.springframework.data.domain.Pageable;
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
     * Retrieves all sessions for a specific case (Mediation + Conciliation),
     * ordered by scheduled start date ascending.
     */
    List<CaseSession> findByCasesIdOrderByScheduledStartAsc(Long caseId);

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

    @Query("""
    SELECT new com.barangay.cases.blotter.dto.QuickMediationSummaryDto(
        s.id,
        c.caseNumber,
        CONCAT(
            COALESCE(compPerson.firstName, ''), ' ', COALESCE(compPerson.lastName, ''),
            ' vs. ',
            COALESCE(respPerson.firstName, ''), ' ', COALESCE(respPerson.lastName, '')
        ),
        s.summonNumber,
        s.scheduledStart,
        s.venue
    )
    FROM CaseSession s
    JOIN s.cases c
    LEFT JOIN c.complainant comp
    LEFT JOIN comp.person compPerson
    LEFT JOIN c.respondent resp
    LEFT JOIN resp.person respPerson
    WHERE (c.department IS NULL OR UPPER(c.department.name) = 'BLOTTER')
      AND (c.isArchived IS NULL OR c.isArchived = false)
      AND (c.isDeleted IS NULL OR c.isDeleted = false)
      AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
      AND s.hearingType = 'MEDIATION'
      AND s.status = 'SCHEDULED'
      AND s.scheduledStart >= :startOfDay
      AND s.scheduledStart <= :endOfDay
    ORDER BY s.scheduledStart ASC
""")
    List<QuickMediationSummaryDto> findQuickTodayMediations(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            Pageable pageable
    );

    /**
     * Explicit JPQL version of findAllByCasesIdAndStatus.
     * Useful if you want guaranteed explicit JOIN and ordering by scheduled start time.
     *
     * @param caseId database identifier of the parent case
     * @param status target session status
     * @return list of matching {@link CaseSession} entities ordered by scheduledStart
     */
    @Query("""
        SELECT s FROM CaseSession s
        WHERE s.cases.id = :caseId
          AND s.status = :status
        ORDER BY s.scheduledStart ASC
    """)
    List<CaseSession> findSessionsByCaseIdAndStatus(
            @Param("caseId") Long caseId,
            @Param("status") SessionStatus status
    );

    @Query("""
        SELECT s FROM CaseSession s
        WHERE s.cases.id IN :caseIds
          AND s.status = :status
        ORDER BY s.scheduledStart ASC
    """)
    List<CaseSession> findSessionsByCaseIdInAndStatus(
            @Param("caseIds") List<Long> caseIds,
            @Param("status") SessionStatus status
    );
}
