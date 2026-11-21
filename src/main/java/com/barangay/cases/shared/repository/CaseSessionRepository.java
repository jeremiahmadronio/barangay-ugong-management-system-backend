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

    List<CaseSession> findByCasesIdOrderByScheduledStartAsc(Long caseId);

    List<CaseSession> findByCasesIdAndHearingTypeOrderBySummonNumberAsc(
            Long caseId, SessionType hearingType);

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
        SELECT COUNT(DISTINCT s.id)
        FROM CaseSession s
        JOIN s.cases c
        WHERE (c.luponReferral IS NOT NULL OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON') OR EXISTS (SELECT 1 FROM Case child WHERE child.previousCaseNumber = c.caseNumber AND (child.luponReferral IS NOT NULL OR (child.department IS NOT NULL AND UPPER(child.department.name) = 'LUPON'))))
          AND (c.isArchived IS NULL OR c.isArchived = false)
          AND (c.isDeleted IS NULL OR c.isDeleted = false)
          AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
          AND s.status = :sessionStatus
          AND s.scheduledStart >= :startOfWeek
          AND s.scheduledStart <= :endOfWeek
    """)
    long countUpcomingLuponSessionsThisWeek(
            @Param("startOfWeek") LocalDateTime startOfWeek,
            @Param("endOfWeek") LocalDateTime endOfWeek,
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
    WHERE (s.hearingType = com.barangay.zshared.enumerated.SessionType.CONCILIATION
           OR c.luponReferral IS NOT NULL
           OR c.caseStatus = com.barangay.zshared.enumerated.CaseStatus.UNDER_CONCILIATION
           OR (c.department IS NOT NULL AND UPPER(c.department.name) = 'LUPON'))
      AND (c.isArchived IS NULL OR c.isArchived = false)
      AND (c.isDeleted IS NULL OR c.isDeleted = false)
      AND (c.isPermanentDeleted IS NULL OR c.isPermanentDeleted = false)
      AND s.status = com.barangay.zshared.enumerated.SessionStatus.SCHEDULED
      AND s.scheduledStart >= :startOfDay
      AND s.scheduledStart <= :endOfDay
    ORDER BY s.scheduledStart ASC
""")
    List<QuickMediationSummaryDto> findQuickTodayLuponConciliations(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            Pageable pageable
    );
}
