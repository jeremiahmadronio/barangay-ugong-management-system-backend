package com.barangay.cases.bcpc.service;

import com.barangay.cases.bcpc.dto.BcpcInterventionCancelRequest;
import com.barangay.cases.bcpc.dto.BcpcInterventionCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcInterventionDto;
import com.barangay.cases.bcpc.dto.BcpcInterventionProgressRequest;
import com.barangay.cases.bcpc.dto.BcpcInterventionUpdateRequest;
import com.barangay.cases.bcpc.model.BcpcCaseDetails;
import com.barangay.cases.bcpc.model.BcpcIntervention;
import com.barangay.cases.bcpc.repository.BcpcCaseDetailsRepository;
import com.barangay.cases.bcpc.repository.BcpcInterventionRepository;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.TimelineEventType;
import com.barangay.zshared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Service managing community-based and institutional intervention programs and progress tracking for BCPC cases.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BcpcInterventionService {

    private final CaseRepository caseRepository;
    private final BcpcCaseDetailsRepository bcpcCaseDetailsRepository;
    private final BcpcInterventionRepository bcpcInterventionRepository;
    private final CaseTimelineService caseTimelineService;

    @Transactional(readOnly = true)
    public List<BcpcInterventionDto> getInterventionsByCaseId(Long caseId) {
        caseRepository.findById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found with ID: " + caseId));

        BcpcCaseDetails details = bcpcCaseDetailsRepository.findByCasesId(caseId).orElse(null);
        if (details == null) {
            return List.of();
        }

        return bcpcInterventionRepository.findByBcpcCaseDetailsIdOrderByCreatedAtDesc(details.getId())
                .stream()
                .map(this::toInterventionDto)
                .toList();
    }

    @Transactional
    public BcpcInterventionDto createIntervention(Long caseId, BcpcInterventionCreateRequest request) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found with ID: " + caseId));

        if (aCase.getCaseStatus() != null) {
            String st = aCase.getCaseStatus().name();
            if ("RESOLVED".equalsIgnoreCase(st) || "CLOSED".equalsIgnoreCase(st) || "SETTLED".equalsIgnoreCase(st)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot initiate intervention for a resolved or closed case.");
            }
        }

        BcpcCaseDetails details = bcpcCaseDetailsRepository.findByCasesId(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "BCPC details not found for case ID: " + caseId));

        BcpcIntervention intervention = new BcpcIntervention();
        intervention.setBcpcCaseDetails(details);
        intervention.setInterventionType(request.getInterventionType());
        intervention.setProgramTitle(request.getProgramTitle().trim());
        intervention.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        intervention.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now());
        intervention.setEndDate(request.getEndDate());
        intervention.setStatus("ONGOING");
        intervention.setFacilitatorOrAgency(request.getFacilitatorOrAgency() != null && !request.getFacilitatorOrAgency().isBlank()
                ? request.getFacilitatorOrAgency().trim() : "Barangay BCPC Committee");

        User currentUser = SecurityUtils.getCurrentUser();
        String officerName = getOfficerDisplayName(currentUser);

        if (request.getInitialNotes() != null && !request.getInitialNotes().isBlank()) {
            String initialEntry = "[" + LocalDate.now() + " - " + officerName + " (Program Established)]\n" + request.getInitialNotes().trim();
            intervention.setProgressNotes(initialEntry);
        }

        BcpcIntervention saved = bcpcInterventionRepository.save(intervention);

        if (aCase.getCaseStatus() == CaseStatus.PENDING) {
            aCase.setCaseStatus(CaseStatus.ONGOING);
            caseRepository.save(aCase);
        }

        String typeLabel = request.getInterventionType() != null ? request.getInterventionType().name().replace('_', ' ') : "Community-Based";
        caseTimelineService.logEvent(
                aCase,
                TimelineEventType.INTERVENTION_RECORDED,
                "Intervention Program Initiated",
                "New " + typeLabel + " program '" + request.getProgramTitle().trim() + "' initiated with " + intervention.getFacilitatorOrAgency() + ".",
                currentUser
        );

        return toInterventionDto(saved);
    }

    @Transactional
    public BcpcInterventionDto updateIntervention(Long caseId, Long interventionId, BcpcInterventionUpdateRequest request) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found with ID: " + caseId));

        BcpcIntervention intervention = bcpcInterventionRepository.findById(interventionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intervention not found with ID: " + interventionId));

        intervention.setInterventionType(request.getInterventionType());
        intervention.setProgramTitle(request.getProgramTitle().trim());
        intervention.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        intervention.setStartDate(request.getStartDate());
        intervention.setEndDate(request.getEndDate());
        intervention.setFacilitatorOrAgency(request.getFacilitatorOrAgency() != null ? request.getFacilitatorOrAgency().trim() : null);

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            intervention.setStatus(request.getStatus().toUpperCase().trim());
        }

        BcpcIntervention saved = bcpcInterventionRepository.save(intervention);

        User currentUser = SecurityUtils.getCurrentUser();
        caseTimelineService.logEvent(
                aCase,
                TimelineEventType.INTERVENTION_RECORDED,
                "Intervention Plan Updated",
                "Program '" + saved.getProgramTitle() + "' details were updated.",
                currentUser
        );

        return toInterventionDto(saved);
    }

    @Transactional
    public BcpcInterventionDto recordInterventionProgress(Long caseId, Long interventionId, BcpcInterventionProgressRequest request) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found with ID: " + caseId));

        BcpcIntervention intervention = bcpcInterventionRepository.findById(interventionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intervention not found with ID: " + interventionId));

        User currentUser = SecurityUtils.getCurrentUser();
        String officerName = request.getConductedBy() != null && !request.getConductedBy().isBlank()
                ? request.getConductedBy().trim()
                : getOfficerDisplayName(currentUser);

        LocalDate sessionDate = request.getSessionDate() != null ? request.getSessionDate() : LocalDate.now();
        String statusLabel = request.getStatus() != null && !request.getStatus().isBlank()
                ? request.getStatus().toUpperCase().trim()
                : intervention.getStatus();

        String newEntry = "[" + sessionDate + " - " + officerName + " (" + statusLabel + ")]\n" + request.getProgressNotes().trim();

        if (intervention.getProgressNotes() != null && !intervention.getProgressNotes().isBlank()) {
            intervention.setProgressNotes(intervention.getProgressNotes() + "\n\n" + newEntry);
        } else {
            intervention.setProgressNotes(newEntry);
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            intervention.setStatus(statusLabel);
        }

        BcpcIntervention saved = bcpcInterventionRepository.save(intervention);

        if (aCase.getCaseStatus() == CaseStatus.PENDING) {
            aCase.setCaseStatus(CaseStatus.ONGOING);
            caseRepository.save(aCase);
        }

        String eventTitle = "COMPLETED".equalsIgnoreCase(statusLabel)
                ? "Intervention Program Completed"
                : ("TERMINATED".equalsIgnoreCase(statusLabel) ? "Intervention Program Terminated" : "Intervention Progress Logged");

        caseTimelineService.logEvent(
                aCase,
                TimelineEventType.INTERVENTION_RECORDED,
                eventTitle,
                "Session date: " + sessionDate + " (" + officerName + "). " + request.getProgressNotes().trim(),
                currentUser
        );

        return toInterventionDto(saved);
    }

    @Transactional
    public BcpcInterventionDto cancelIntervention(Long caseId, Long interventionId, BcpcInterventionCancelRequest request) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found with ID: " + caseId));

        BcpcIntervention intervention = bcpcInterventionRepository.findById(interventionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Intervention not found with ID: " + interventionId));

        intervention.setStatus("CANCELLED");
        String cancellationNote = "[" + LocalDate.now() + " - CANCELLED]\nReason: " + (request.getReason() != null ? request.getReason().trim() : "Cancelled by officer");
        if (intervention.getProgressNotes() != null && !intervention.getProgressNotes().isBlank()) {
            intervention.setProgressNotes(intervention.getProgressNotes() + "\n\n" + cancellationNote);
        } else {
            intervention.setProgressNotes(cancellationNote);
        }

        BcpcIntervention saved = bcpcInterventionRepository.save(intervention);

        User currentUser = SecurityUtils.getCurrentUser();
        caseTimelineService.logEvent(
                aCase,
                TimelineEventType.INTERVENTION_RECORDED,
                "Intervention Program Cancelled",
                "Program '" + saved.getProgramTitle() + "' was cancelled. Reason: " + request.getReason(),
                currentUser
        );

        return toInterventionDto(saved);
    }

    private String getOfficerDisplayName(User user) {
        if (user == null) return "BCPC Officer";
        if (user.getPerson() != null) {
            String first = user.getPerson().getFirstName() != null ? user.getPerson().getFirstName().trim() : "";
            String last = user.getPerson().getLastName() != null ? user.getPerson().getLastName().trim() : "";
            String full = (first + " " + last).trim();
            if (!full.isEmpty()) return full;
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return "BCPC Officer";
    }

    public BcpcInterventionDto toInterventionDto(BcpcIntervention entity) {
        String typeDisplay = entity.getInterventionType() != null
                ? entity.getInterventionType().name().replace('_', ' ')
                : null;
        return BcpcInterventionDto.builder()
                .id(entity.getId())
                .caseId(entity.getBcpcCaseDetails() != null && entity.getBcpcCaseDetails().getCases() != null
                        ? entity.getBcpcCaseDetails().getCases().getId() : null)
                .bcpcCaseDetailsId(entity.getBcpcCaseDetails() != null ? entity.getBcpcCaseDetails().getId() : null)
                .interventionType(entity.getInterventionType())
                .interventionTypeDisplay(typeDisplay)
                .programTitle(entity.getProgramTitle())
                .description(entity.getDescription())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .status(entity.getStatus())
                .facilitatorOrAgency(entity.getFacilitatorOrAgency())
                .progressNotes(entity.getProgressNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
