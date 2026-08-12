package com.barangay.cases.blotter.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barangay.cases.blotter.dto.MediationSessionRequest;
import com.barangay.cases.blotter.dto.MediationSessionResponse;
import com.barangay.cases.blotter.dto.SessionFollowUpRequest;
import com.barangay.cases.blotter.dto.SessionFollowUpResponse;
import com.barangay.cases.blotter.dto.SessionMinutesRequest;
import com.barangay.cases.blotter.dto.SessionMinutesResponse;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.CaseSession;
import com.barangay.cases.shared.model.SessionFollowUp;
import com.barangay.cases.shared.model.SessionMinutes;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.repository.CaseSessionRepository;
import com.barangay.cases.shared.repository.SessionFollowUpRepository;
import com.barangay.cases.shared.repository.SessionMinutesRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
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

/**
 * Service for managing Mediation Sessions, hearing schedules, session minutes, and session follow-ups.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediationSessionService {

    private final CaseRepository caseRepository;
    private final CaseSessionRepository caseSessionRepository;
    private final SessionMinutesRepository sessionMinutesRepository;
    private final SessionFollowUpRepository sessionFollowUpRepository;
    private final CaseTimelineService caseTimelineService;

    /**
     * Lists all MEDIATION-type sessions for a given blotter case.
     *
     * @param caseId the blotter case ID
     * @return ordered list of {@link MediationSessionResponse}
     */
    @Transactional(readOnly = true)
    public List<MediationSessionResponse> getMediationSessions(Long caseId) {
        caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Blotter case not found: " + caseId));

        List<CaseSession> sessions = caseSessionRepository
                .findByCasesIdAndHearingTypeOrderBySummonNumberAsc(caseId, SessionType.MEDIATION);

        return sessions.stream().map(this::toMediationResponse).toList();
    }

    /**
     * Schedules a new Mediation session for a given blotter case.
     * Automatically assigns the next sequential hearing number.
     * Also updates the case status to UNDER_MEDIATION.
     *
     * @param caseId  the blotter case ID
     * @param request the schedule request payload
     * @return the created {@link MediationSessionResponse}
     */
    @Transactional
    public MediationSessionResponse scheduleMediation(Long caseId, MediationSessionRequest request) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Blotter case not found: " + caseId));

        List<CaseSession> existing = caseSessionRepository
                .findByCasesIdAndHearingTypeOrderBySummonNumberAsc(caseId, SessionType.MEDIATION);
        short nextNumber = (short) (existing.size() + 1);

        CaseSession session = new CaseSession();
        session.setCases(aCase);
        session.setHearingType(SessionType.MEDIATION);
        session.setSummonNumber(nextNumber);
        session.setScheduledStart(request.getScheduledStart());
        session.setScheduledEnd(request.getScheduledEnd());
        session.setVenue(request.getVenue() != null ? request.getVenue() : "Barangay Hall");
        session.setNotes(request.getNotes());
        session.setStatus(SessionStatus.SCHEDULED);

        CaseSession saved = caseSessionRepository.save(session);

        if (aCase.getCaseStatus() == CaseStatus.PENDING || aCase.getCaseStatus() == null) {
            aCase.setCaseStatus(CaseStatus.UNDER_MEDIATION);
            caseRepository.save(aCase);
        }

        try {
            String dateFormatted = formatDateTimeReadable(request.getScheduledStart());
            caseTimelineService.logEvent(
                    aCase,
                    TimelineEventType.SUMMON_ISSUED,
                    "Mediation Scheduled (Summon #" + nextNumber + ")",
                    "Hearing #" + nextNumber + " scheduled for " + dateFormatted + " at " + session.getVenue() + ".",
                    SecurityUtils.getCurrentUser()
            );
        } catch (Exception e) {
            log.warn("Failed to log timeline event for scheduled mediation [caseId={}]: {}", aCase.getId(), e.getMessage(), e);
        }

        return toMediationResponse(saved);
    }

    /**
     * Cancels a scheduled Mediation session.
     *
     * @param sessionId the session ID to cancel
     * @return the updated {@link MediationSessionResponse}
     */
    @Transactional
    public MediationSessionResponse cancelMediation(Long sessionId) {
        CaseSession session = caseSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mediation session not found: " + sessionId));

        session.setStatus(SessionStatus.CANCELLED);
        CaseSession saved = caseSessionRepository.save(session);

        try {
            if (session.getCases() != null) {
                caseTimelineService.logEvent(
                        session.getCases(),
                        TimelineEventType.STATUS_CHANGED,
                        "Mediation Hearing Cancelled",
                        "Mediation session (Summon #" + session.getSummonNumber() + ") was cancelled.",
                        SecurityUtils.getCurrentUser()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to log timeline event for cancelled mediation [sessionId={}]: {}", sessionId, e.getMessage(), e);
        }

        return toMediationResponse(saved);
    }

    /**
     * Records or updates minutes for a mediation session.
     * Also updates session status to COMPLETED and case status to SETTLED if outcome is SETTLED.
     *
     * @param sessionId the session ID
     * @param req       the session minutes request payload
     * @return updated {@link MediationSessionResponse}
     */
    @Transactional
    public MediationSessionResponse recordSessionMinutes(Long sessionId, SessionMinutesRequest req) {
        CaseSession session = caseSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mediation session not found: " + sessionId));

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

        Case aCase = session.getCases();
        if (aCase != null) {
            if (outcome == SessionOutcome.SETTLED) {
                aCase.setCaseStatus(CaseStatus.SETTLED);
                aCase.setSettlementTerms(req.getSettlementTerms());
                aCase.setSettledAt(LocalDateTime.now());
                caseRepository.save(aCase);
            }
        }

        try {
            if (aCase != null) {
                TimelineEventType eventType = outcome == SessionOutcome.SETTLED
                        ? TimelineEventType.CASE_SETTLED
                        : TimelineEventType.HEARING_CONDUCTED;
                String title = outcome == SessionOutcome.SETTLED
                        ? "Case Settled in Mediation"
                        : "Mediation Hearing Conducted (Outcome: " + outcome.name() + ")";
                String desc = "Summon #" + session.getSummonNumber() + " minutes recorded. " + (req.getHearingNotes() != null ? req.getHearingNotes() : "");

                caseTimelineService.logEvent(aCase, eventType, title, desc, SecurityUtils.getCurrentUser());
            }
        } catch (Exception e) {
            log.warn("Failed to log timeline event for recorded session minutes [sessionId={}]: {}", sessionId, e.getMessage(), e);
        }

        return toMediationResponse(session);
    }

    /**
     * Gets recorded minutes for a mediation session.
     *
     * @param sessionId the session ID
     * @return {@link SessionMinutesResponse}
     */
    @Transactional(readOnly = true)
    public SessionMinutesResponse getMediationMinutes(Long sessionId) {
        SessionMinutes minutes = sessionMinutesRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("No recorded minutes for session: " + sessionId));

        return toMinutesResponse(minutes);
    }

    /**
     * Adds a follow-up note to a completed mediation session.
     *
     * @param sessionId the session ID
     * @param req       the session follow-up request payload
     * @return {@link SessionFollowUpResponse}
     */
    @Transactional
    public SessionFollowUpResponse addSessionFollowUp(Long sessionId, SessionFollowUpRequest req) {
        CaseSession session = caseSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Mediation session not found: " + sessionId));

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
                        "Session Follow-Up Added",
                        "Follow-up recorded for Summon #" + session.getSummonNumber() + ": " + req.getRemarks(),
                        SecurityUtils.getCurrentUser()
                );
            }
        } catch (Exception e) {
            log.warn("Failed to log timeline event for session follow-up [sessionId={}]: {}", sessionId, e.getMessage(), e);
        }

        return toFollowUpResponse(saved);
    }

    /**
     * Lists all follow-up notes for a mediation session.
     *
     * @param sessionId the session ID
     * @return list of {@link SessionFollowUpResponse}
     */
    @Transactional(readOnly = true)
    public List<SessionFollowUpResponse> getSessionFollowUps(Long sessionId) {
        return sessionFollowUpRepository.findBySessionIdOrderByCreatedAtDesc(sessionId)
                .stream().map(this::toFollowUpResponse).toList();
    }

    /**
     * Maps a {@link CaseSession} entity to a {@link MediationSessionResponse} DTO.
     */
    private MediationSessionResponse toMediationResponse(CaseSession s) {
        var optMinutes = sessionMinutesRepository.findFirstBySessionIdOrderByCreatedAtDesc(s.getId());
        boolean hasMins = optMinutes.isPresent();
        SessionMinutesResponse minsDto = optMinutes.map(this::toMinutesResponse).orElse(null);

        List<SessionFollowUpResponse> followUpList = sessionFollowUpRepository.findBySessionIdOrderByCreatedAtDesc(s.getId())
                .stream().map(this::toFollowUpResponse).toList();

        return MediationSessionResponse.builder()
                .id(s.getId())
                .caseId(s.getCases() != null ? s.getCases().getId() : null)
                .hearingNumber(s.getSummonNumber())
                .scheduledStart(s.getScheduledStart())
                .scheduledEnd(s.getScheduledEnd())
                .venue(s.getVenue())
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .notes(s.getNotes())
                .hasMinutes(hasMins)
                .minutes(minsDto)
                .followUps(followUpList)
                .createdAt(s.getCreatedAt())
                .build();
    }

    /**
     * Maps a {@link SessionMinutes} entity to {@link SessionMinutesResponse}.
     */
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

    /**
     * Maps a {@link SessionFollowUp} entity to {@link SessionFollowUpResponse}.
     */
    private SessionFollowUpResponse toFollowUpResponse(SessionFollowUp f) {
        String recordedBy = null;
        if (f.getRecordedBy() != null) {
            User u = f.getRecordedBy();
            if (u.getPerson() != null) {
                String first = u.getPerson().getFirstName() != null ? u.getPerson().getFirstName() : "";
                String last  = u.getPerson().getLastName()  != null ? u.getPerson().getLastName()  : "";
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

    private String formatDateTimeReadable(LocalDateTime dt) {
        if (dt == null) return "";
        try {
            return dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"));
        } catch (Exception e) {
            return dt.toString();
        }
    }

}
