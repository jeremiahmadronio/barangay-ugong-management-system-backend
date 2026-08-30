package com.barangay.cases.lupon.service;

import com.barangay.cases.blotter.dto.SessionFollowUpRequest;
import com.barangay.cases.blotter.dto.SessionFollowUpResponse;
import com.barangay.cases.blotter.dto.SessionMinutesResponse;
import com.barangay.cases.lupon.dto.*;
import com.barangay.cases.lupon.model.LuponPanelMember;
import com.barangay.cases.lupon.model.PanelAttendance;
import com.barangay.cases.lupon.repository.LuponPanelMemberRepository;
import com.barangay.cases.lupon.repository.PanelAttendanceRepository;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.CaseSession;
import com.barangay.cases.shared.model.SessionFollowUp;
import com.barangay.cases.shared.model.SessionMinutes;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.repository.CaseSessionRepository;
import com.barangay.cases.shared.repository.SessionFollowUpRepository;
import com.barangay.cases.shared.repository.SessionMinutesRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.people.officer.model.Officer;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.SessionOutcome;
import com.barangay.zshared.enumerated.SessionStatus;
import com.barangay.zshared.enumerated.SessionType;
import com.barangay.zshared.enumerated.TimelineEventType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConciliationService {

    private final CaseRepository caseRepository;
    private final CaseSessionRepository caseSessionRepository;
    private final SessionMinutesRepository sessionMinutesRepository;
    private final SessionFollowUpRepository sessionFollowUpRepository;
    private final LuponPanelMemberRepository luponPanelMemberRepository;
    private final PanelAttendanceRepository panelAttendanceRepository;
    private final CaseTimelineService caseTimelineService;

    /**
     * Retrieves all sessions for a case, properly categorizing and ordering them:
     * - Mediation sessions (conducted before referral to Lupon) are labeled Mediation 1, 2...
     * - Conciliation sessions (conducted in Lupon / after referral) are labeled Conciliation Hearing 1, 2...
     * - Includes assigned Pangkat panel members and their attendance for conciliation sessions.
     *
     * @param caseId database identifier of the case
     * @return chronological list of {@link ConciliationSessionResponse}
     */
    @Transactional(readOnly = true)
    public List<ConciliationSessionResponse> getConciliationSessions(Long caseId) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));

        LocalDateTime referredAt = null;
        if (aCase.getLuponReferral() != null) {
            referredAt = aCase.getLuponReferral().getReferredAt();
        }

        // Fetch all sessions ordered by creation date
        List<CaseSession> allSessions = caseSessionRepository.findByCasesIdOrderByScheduledStartAsc(caseId);
        if (allSessions.isEmpty()) {
            return Collections.emptyList();
        }

        // Sort reliably: by createdAt (or scheduledStart fallback)
        allSessions = allSessions.stream()
                .sorted(Comparator.comparing((CaseSession s) -> s.getCreatedAt() != null ? s.getCreatedAt() : s.getScheduledStart()))
                .toList();

        List<LuponPanelMember> panelMembers = luponPanelMemberRepository.findByCasesId(caseId);

        List<ConciliationSessionResponse> responseList = new ArrayList<>();
        short mediationCounter = 0;
        short conciliationCounter = 0;

        for (CaseSession s : allSessions) {
            boolean isConciliation = s.getHearingType() == SessionType.CONCILIATION
                    || (referredAt != null && s.getCreatedAt() != null && !s.getCreatedAt().isBefore(referredAt))
                    || "Lupon".equalsIgnoreCase(s.getVenue());

            short assignedNumber;
            String hearingTypeStr;
            String sessionTitle;

            if (isConciliation) {
                conciliationCounter++;
                assignedNumber = conciliationCounter;
                hearingTypeStr = "CONCILIATION";
                sessionTitle = "Conciliation Hearing " + conciliationCounter;
            } else {
                mediationCounter++;
                assignedNumber = s.getSummonNumber() != null ? s.getSummonNumber() : mediationCounter;
                hearingTypeStr = "MEDIATION";
                sessionTitle = "Mediation " + assignedNumber;
            }

            // Panel Attendances (only for conciliation sessions)
            List<PanelAttendanceResponse> attendances = new ArrayList<>();
            if (isConciliation && !panelMembers.isEmpty()) {
                Map<Long, PanelAttendance> attendanceMap = new HashMap<>();
                panelAttendanceRepository.findBySessionId(s.getId()).forEach(a -> {
                    if (a.getPanelMember() != null) {
                        attendanceMap.put(a.getPanelMember().getId(), a);
                    }
                });

                for (LuponPanelMember m : panelMembers) {
                    PanelAttendance pa = attendanceMap.get(m.getId());
                    attendances.add(PanelAttendanceResponse.builder()
                            .id(pa != null ? pa.getId() : null)
                            .panelMemberId(m.getId())
                            .officerId(m.getOfficer() != null ? m.getOfficer().getId() : null)
                            .officerName(formatOfficerFullName(m.getOfficer()))
                            .position(m.getPosition())
                            .isPresent(pa != null && Boolean.TRUE.equals(pa.getIsPresent()))
                            .build());
                }
            }

            var optMinutes = sessionMinutesRepository.findFirstBySessionIdOrderByCreatedAtDesc(s.getId());
            boolean hasMins = optMinutes.isPresent();
            SessionMinutesResponse minsDto = optMinutes.map(this::toMinutesResponse).orElse(null);

            List<SessionFollowUpResponse> followUpList = sessionFollowUpRepository.findBySessionIdOrderByCreatedAtDesc(s.getId())
                    .stream().map(this::toFollowUpResponse).toList();

            responseList.add(ConciliationSessionResponse.builder()
                    .id(s.getId())
                    .caseId(caseId)
                    .hearingNumber(assignedNumber)
                    .hearingType(hearingTypeStr)
                    .sessionTitle(sessionTitle)
                    .isConciliation(isConciliation)
                    .scheduledStart(s.getScheduledStart())
                    .scheduledEnd(s.getScheduledEnd())
                    .venue(s.getVenue())
                    .status(s.getStatus() != null ? s.getStatus().name() : null)
                    .notes(s.getNotes())
                    .hasMinutes(hasMins)
                    .minutes(minsDto)
                    .followUps(followUpList)
                    .panelAttendances(attendances)
                    .createdAt(s.getCreatedAt())
                    .build());
        }

        return responseList;
    }

    /**
     * Schedules a new Conciliation Hearing in Lupon.
     * Automatically assigns the next sequential conciliation hearing number
     * and links assigned Pangkat panel members.
     *
     * @param caseId database identifier of the case
     * @param req    the schedule request payload
     * @return the created {@link ConciliationSessionResponse}
     */
    @Transactional
    public ConciliationSessionResponse scheduleConciliation(Long caseId, ScheduleConciliationRequest req) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));

        // Find max summon number across ALL sessions of this case to avoid unique constraint (case_id, summon_number)
        List<CaseSession> allSessions = caseSessionRepository.findByCasesIdOrderByScheduledStartAsc(caseId);
        short maxNumber = 0;
        long conciliationCount = 0;
        for (CaseSession s : allSessions) {
            if (s.getSummonNumber() != null && s.getSummonNumber() > maxNumber) {
                maxNumber = s.getSummonNumber();
            }
            if (s.getHearingType() == SessionType.CONCILIATION) {
                conciliationCount++;
            }
        }
        short nextSummonNumber = (short) (maxNumber + 1);
        short conciliationHearingNumber = (short) (conciliationCount + 1);

        CaseSession session = new CaseSession();
        session.setCases(aCase);
        session.setHearingType(SessionType.CONCILIATION);
        session.setSummonNumber(nextSummonNumber);
        session.setScheduledStart(req.getScheduledStart());
        session.setScheduledEnd(req.getScheduledEnd());
        session.setVenue(req.getVenue() != null && !req.getVenue().isBlank() ? req.getVenue() : "Lupon");
        session.setNotes(req.getNotes());
        session.setStatus(SessionStatus.SCHEDULED);

        CaseSession saved = caseSessionRepository.save(session);

        // Ensure case status is UNDER_CONCILIATION
        if (aCase.getCaseStatus() != CaseStatus.UNDER_CONCILIATION) {
            aCase.setCaseStatus(CaseStatus.UNDER_CONCILIATION);
            caseRepository.save(aCase);
        }

        // Initialize Panel Attendance records for all assigned Pangkat members
        List<LuponPanelMember> panelMembers = luponPanelMemberRepository.findByCasesId(caseId);
        List<PanelAttendance> attendancesToSave = new ArrayList<>();
        Set<Long> presentIds = req.getPresentPanelMemberIds() != null
                ? new HashSet<>(req.getPresentPanelMemberIds())
                : Collections.emptySet();

        for (LuponPanelMember m : panelMembers) {
            PanelAttendance pa = new PanelAttendance();
            pa.setSession(saved);
            pa.setPanelMember(m);
            pa.setIsPresent(presentIds.contains(m.getId()));
            attendancesToSave.add(pa);
        }
        if (!attendancesToSave.isEmpty()) {
            panelAttendanceRepository.saveAll(attendancesToSave);
        }

        try {
            String dateFormatted = formatDateTimeReadable(req.getScheduledStart());
            caseTimelineService.logEvent(
                    aCase,
                    TimelineEventType.SUMMON_ISSUED,
                    "Conciliation Hearing Scheduled (Hearing #" + conciliationHearingNumber + ")",
                    "Conciliation hearing #" + conciliationHearingNumber + " scheduled for " + dateFormatted + " at " + saved.getVenue() + ".",
                    SecurityUtils.getCurrentUser()
            );
        } catch (Exception e) {
            log.warn("Failed to log timeline event for scheduled conciliation [caseId={}]: {}", caseId, e.getMessage());
        }

        // Re-read with full attendance objects
        return getConciliationSessions(caseId).stream()
                .filter(cs -> cs.getId().equals(saved.getId()))
                .findFirst()
                .orElseGet(() -> mapBasicResponse(saved, conciliationHearingNumber));
    }

    /**
     * Cancels a scheduled conciliation hearing.
     *
     * @param sessionId the session ID to cancel
     * @return updated {@link ConciliationSessionResponse}
     */
    @Transactional
    public ConciliationSessionResponse cancelConciliation(Long sessionId) {
        CaseSession session = caseSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        session.setStatus(SessionStatus.CANCELLED);
        CaseSession saved = caseSessionRepository.save(session);

        try {
            if (session.getCases() != null) {
                caseTimelineService.logEvent(
                        session.getCases(),
                        TimelineEventType.STATUS_CHANGED,
                        "Conciliation Hearing Cancelled",
                        "Conciliation session (Hearing #" + session.getSummonNumber() + ") was cancelled.",
                        SecurityUtils.getCurrentUser()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to log timeline event for cancelled conciliation [sessionId={}]: {}", sessionId, e.getMessage());
        }

        return getConciliationSessions(saved.getCases().getId()).stream()
                .filter(cs -> cs.getId().equals(saved.getId()))
                .findFirst()
                .orElseGet(() -> mapBasicResponse(saved, saved.getSummonNumber()));
    }

    /**
     * Records or updates minutes and Pangkat attendance for a conciliation session.
     *
     * @param sessionId session ID
     * @param req       payload containing minutes and panel attendance
     * @return updated {@link ConciliationSessionResponse}
     */
    @Transactional
    public ConciliationSessionResponse recordConciliationMinutes(Long sessionId, RecordConciliationMinutesRequest req) {
        CaseSession session = caseSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Conciliation session not found: " + sessionId));

        SessionMinutes minutes = sessionMinutesRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId)
                .orElseGet(() -> {
                    SessionMinutes m = new SessionMinutes();
                    m.setSession(session);
                    return m;
                });

        minutes.setComplainantPresent(Boolean.TRUE.equals(req.getComplainantPresent()));
        minutes.setRespondentPresent(Boolean.TRUE.equals(req.getRespondentPresent()));
        minutes.setHearingNotes(req.getHearingNotes());
        minutes.setSettlementTerms(req.getSettlementTerms());

        SessionOutcome outcome = SessionOutcome.FAILED;
        if ("SETTLED".equalsIgnoreCase(req.getOutcome())) {
            outcome = SessionOutcome.SETTLED;
        } else if ("POSTPONED".equalsIgnoreCase(req.getOutcome())) {
            outcome = SessionOutcome.POSTPONED;
        }
        minutes.setOutcome(outcome);
        sessionMinutesRepository.save(minutes);

        session.setStatus(SessionStatus.COMPLETED);
        caseSessionRepository.save(session);

        // Update Panel Attendance
        if (req.getPanelAttendance() != null && !req.getPanelAttendance().isEmpty()) {
            List<PanelAttendance> existing = panelAttendanceRepository.findBySessionId(sessionId);
            Map<Long, PanelAttendance> map = new HashMap<>();
            existing.forEach(a -> {
                if (a.getPanelMember() != null) {
                    map.put(a.getPanelMember().getId(), a);
                }
            });

            for (Map.Entry<Long, Boolean> entry : req.getPanelAttendance().entrySet()) {
                Long memberId = entry.getKey();
                Boolean isPres = Boolean.TRUE.equals(entry.getValue());

                PanelAttendance pa = map.get(memberId);
                if (pa != null) {
                    pa.setIsPresent(isPres);
                    panelAttendanceRepository.save(pa);
                } else {
                    LuponPanelMember member = luponPanelMemberRepository.findById(memberId).orElse(null);
                    if (member != null) {
                        PanelAttendance newPa = new PanelAttendance();
                        newPa.setSession(session);
                        newPa.setPanelMember(member);
                        newPa.setIsPresent(isPres);
                        panelAttendanceRepository.save(newPa);
                    }
                }
            }
        }

        Case aCase = session.getCases();
        if (aCase != null && outcome == SessionOutcome.SETTLED) {
            aCase.setCaseStatus(CaseStatus.SETTLED);
            aCase.setSettlementTerms(req.getSettlementTerms());
            aCase.setSettledAt(LocalDateTime.now());
            caseRepository.save(aCase);
        }

        try {
            if (aCase != null) {
                TimelineEventType eventType = outcome == SessionOutcome.SETTLED
                        ? TimelineEventType.CASE_SETTLED
                        : TimelineEventType.HEARING_CONDUCTED;
                String title = outcome == SessionOutcome.SETTLED
                        ? "Case Settled in Conciliation"
                        : "Conciliation Hearing Conducted (Outcome: " + outcome.name() + ")";
                String desc = "Conciliation Hearing #" + session.getSummonNumber() + " minutes recorded. " + (req.getHearingNotes() != null ? req.getHearingNotes() : "");

                caseTimelineService.logEvent(aCase, eventType, title, desc, SecurityUtils.getCurrentUser());
            }
        } catch (Exception e) {
            log.warn("Failed to log timeline event for recorded conciliation minutes [sessionId={}]: {}", sessionId, e.getMessage());
        }

        return getConciliationSessions(session.getCases().getId()).stream()
                .filter(cs -> cs.getId().equals(session.getId()))
                .findFirst()
                .orElseGet(() -> mapBasicResponse(session, session.getSummonNumber()));
    }

    /**
     * Appends a follow-up remark to a conciliation session.
     *
     * @param sessionId the session ID
     * @param req       the follow-up request containing remarks
     * @return {@link SessionFollowUpResponse}
     */
    @Transactional
    public SessionFollowUpResponse addConciliationFollowUp(Long sessionId, SessionFollowUpRequest req) {
        CaseSession session = caseSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        SessionFollowUp followUp = new SessionFollowUp();
        followUp.setSession(session);
        followUp.setRemarks(req.getRemarks());
        followUp.setRecordedBy(SecurityUtils.getCurrentUser());

        SessionFollowUp saved = sessionFollowUpRepository.save(followUp);

        try {
            if (session.getCases() != null) {
                caseTimelineService.logEvent(
                        session.getCases(),
                        TimelineEventType.HEARING_FOLLOWUP,
                        "Conciliation Hearing Follow-Up Added",
                        "Follow-up remarks added for conciliation hearing #" + session.getSummonNumber() + ": " + req.getRemarks(),
                        SecurityUtils.getCurrentUser()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to log timeline event for conciliation follow-up [sessionId={}]: {}", sessionId, e.getMessage());
        }

        return toFollowUpResponse(saved);
    }

    /**
     * Retrieves all follow-up remarks for a conciliation session in reverse chronological order.
     *
     * @param sessionId the session ID
     * @return list of {@link SessionFollowUpResponse}
     */
    @Transactional(readOnly = true)
    public List<SessionFollowUpResponse> getConciliationFollowUps(Long sessionId) {
        return sessionFollowUpRepository.findBySessionIdOrderByCreatedAtDesc(sessionId)
                .stream().map(this::toFollowUpResponse).toList();
    }

    private ConciliationSessionResponse mapBasicResponse(CaseSession s, Short hearingNumber) {
        return ConciliationSessionResponse.builder()
                .id(s.getId())
                .caseId(s.getCases() != null ? s.getCases().getId() : null)
                .hearingNumber(hearingNumber)
                .hearingType("CONCILIATION")
                .sessionTitle("Conciliation Hearing " + hearingNumber)
                .isConciliation(true)
                .scheduledStart(s.getScheduledStart())
                .scheduledEnd(s.getScheduledEnd())
                .venue(s.getVenue())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .notes(s.getNotes())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private SessionMinutesResponse toMinutesResponse(SessionMinutes m) {
        return SessionMinutesResponse.builder()
                .id(m.getId())
                .sessionId(m.getSession() != null ? m.getSession().getId() : null)
                .complainantPresent(m.getComplainantPresent())
                .respondentPresent(m.getRespondentPresent())
                .hearingNotes(m.getHearingNotes())
                .outcome(m.getOutcome() != null ? m.getOutcome().name() : null)
                .settlementTerms(m.getSettlementTerms())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private SessionFollowUpResponse toFollowUpResponse(SessionFollowUp f) {
        String recordedBy = null;
        if (f.getRecordedBy() != null) {
            User u = f.getRecordedBy();
            if (u.getPerson() != null) {
                String first = u.getPerson().getFirstName() != null ? u.getPerson().getFirstName() : "";
                String last = u.getPerson().getLastName() != null ? u.getPerson().getLastName() : "";
                recordedBy = (first + " " + last).trim();
            } else {
                recordedBy = u.getUsername() != null ? u.getUsername() : u.getSystemEmail();
            }
        }
        return SessionFollowUpResponse.builder()
                .id(f.getId())
                .sessionId(f.getSession() != null ? f.getSession().getId() : null)
                .remarks(f.getRemarks())
                .recordedBy(recordedBy)
                .createdAt(f.getCreatedAt())
                .build();
    }

    private String formatOfficerFullName(Officer o) {
        if (o == null || o.getPerson() == null) return "Unknown Officer";
        String fn = o.getPerson().getFirstName() != null ? o.getPerson().getFirstName() : "";
        String ln = o.getPerson().getLastName() != null ? o.getPerson().getLastName() : "";
        return (fn + " " + ln).trim();
    }

    private String formatDateTimeReadable(LocalDateTime dt) {
        if (dt == null) return "";
        try {
            return dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"));
        } catch (Exception e) {
            return dt.toString();
        }
    }
}
