package com.barangay.cases.shared.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barangay.cases.blotter.dto.CaseNoteRequest;
import com.barangay.cases.blotter.dto.CaseNoteResponse;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.CaseNote;
import com.barangay.cases.shared.repository.CaseNoteRepository;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.enumerated.TimelineEventType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.security.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for adding and retrieving case notes for Blotter cases.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaseNoteService {

    private final CaseRepository caseRepository;
    private final CaseNoteRepository caseNoteRepository;
    private final CaseTimelineService caseTimelineService;

    /**
     * Adds a note to a case.
     *
     * @param caseId the case ID
     * @param req    the note request payload
     * @return the created {@link CaseNoteResponse}
     */
    @Transactional
    public CaseNoteResponse addCaseNote(Long caseId, CaseNoteRequest req) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Blotter Case not found with ID: " + caseId));

        CaseNote note = new CaseNote();
        note.setCases(caseEntity);
        note.setNote(req.getNote());
        note.setCreatedBy(SecurityUtils.getCurrentUser());

        CaseNote saved = caseNoteRepository.save(note);

        try {
            caseTimelineService.logEvent(
                    caseEntity,
                    TimelineEventType.NOTE_ADDED,
                    "Case Note Added",
                    req.getNote(),
                    SecurityUtils.getCurrentUser()
            );
        } catch (Exception e) {
            log.warn("Failed to log timeline event for case note [caseId={}]: {}", caseEntity.getId(), e.getMessage(), e);
        }

        return toCaseNoteResponse(saved);
    }

    /**
     * Retrieves all notes for a case.
     *
     * @param caseId the case ID
     * @return list of {@link CaseNoteResponse}
     */
    @Transactional(readOnly = true)
    public List<CaseNoteResponse> getCaseNotes(Long caseId) {
        return caseNoteRepository.findByCasesIdOrderByCreatedAtDesc(caseId)
                .stream().map(this::toCaseNoteResponse).toList();
    }

    /**
     * Maps a {@link CaseNote} entity to {@link CaseNoteResponse}.
     */
    private CaseNoteResponse toCaseNoteResponse(CaseNote n) {
        String createdBy = null;
        if (n.getCreatedBy() != null) {
            User u = n.getCreatedBy();
            if (u.getPerson() != null) {
                String first = u.getPerson().getFirstName() != null ? u.getPerson().getFirstName() : "";
                String last  = u.getPerson().getLastName()  != null ? u.getPerson().getLastName()  : "";
                createdBy = (first + " " + last).trim();
            } else {
                createdBy = u.getUsername() != null ? u.getUsername() : u.getSystemEmail();
            }
        }
        return CaseNoteResponse.builder()
                .id(n.getId())
                .caseId(n.getCases() != null ? n.getCases().getId() : null)
                .note(n.getNote())
                .createdBy(createdBy)
                .createdAt(n.getCreatedAt())
                .build();
    }

}
