package com.barangay.ftjs.service;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.CaseEvidence;
import com.barangay.cases.shared.repository.CaseEvidenceRepository;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.ftjs.dto.FtjsTimelineDto;
import com.barangay.ftjs.dto.LinkedBlotterResponseDTO;
import com.barangay.ftjs.dto.ReIssuanceHistoryDto;
import com.barangay.ftjs.dto.ReIssuanceRequestDto;
import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.ftjs.model.FirstTimeJobSeekerAffidavitOfLoss;
import com.barangay.ftjs.model.FirstTimeJobSeekerTimeLine;
import com.barangay.ftjs.repository.FirstTimeJobSeekerAffidavitOfLossRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerTimeLineRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.enumerated.DocumentReissuanceStatus;
import com.barangay.zshared.enumerated.FtjsStatus;
import com.barangay.zshared.enumerated.RelatedBarangayService;
import com.barangay.zshared.enumerated.TimeLineType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * Service class managing FTJS certificate re-issuance requests, loss affidavits, and event timelines.
 */
public class FtjsReIssuanceService {

    private final FirstTimeJobSeekerRepository firstTimeJobSeekerRepository;
    private final FirstTimeJobSeekerAffidavitOfLossRepository firstTimeJobSeekerAffidavitOfLossRepository;
    private final FirstTimeJobSeekerTimeLineRepository firstTimeJobSeekerTimeLineRepository;
    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final CaseEvidenceRepository caseEvidenceRepository;

    /**
     * Resolves and maps linked blotter details and affidavits for certificate re-issuance.
     */
    @Transactional(readOnly = true)
    public LinkedBlotterResponseDTO getLinkedBlotter(Long ftjsId) {
        List<Case> blotters = caseRepository.findAllByFtjsApplicationIdAndRelatedBarangayServiceOrderByCreatedAtDesc(
                ftjsId,
                RelatedBarangayService.FTJS_REISSUANCE
        );

        if (blotters == null || blotters.isEmpty()) {
            return null;
        }

        Case bc = blotters.stream()
                .filter(b -> b.getDocumentReissuanceStatus() == DocumentReissuanceStatus.PENDING_VERIFICATION ||
                             b.getDocumentReissuanceStatus() == DocumentReissuanceStatus.APPROVED_FOR_REISSUANCE)
                .findFirst()
                .orElse(blotters.get(0));

        boolean isAlreadyReissued = firstTimeJobSeekerAffidavitOfLossRepository.findByFtjsIdOrderByIssuanceNumberDesc(ftjsId)
                .stream().anyMatch(a -> a.getBlotterCase() != null && a.getBlotterCase().getId().equals(bc.getId()));

        List<CaseEvidence> evidences = caseEvidenceRepository.findByCasesId(bc.getId());
        CaseEvidence affidavitRecord = evidences.stream()
                .filter(er -> er.getType() != null && er.getType().getName() != null &&
                        er.getType().getName().toLowerCase().contains("affidavit") &&
                        er.getFiles() != null && er.getFiles().length > 0)
                .findFirst()
                .orElse(evidences.stream().filter(er -> er.getFiles() != null && er.getFiles().length > 0).findFirst().orElse(null));

        boolean hasAffidavit = affidavitRecord != null;
        Long affidavitId = affidavitRecord != null ? affidavitRecord.getId() : null;
        String affidavitFileName = affidavitRecord != null ? affidavitRecord.getFileName() : null;

        LocalDate incidentDate = (bc.getIncidentDetail() != null && bc.getIncidentDetail().getDateOfIncident() != null)
                ? bc.getIncidentDetail().getDateOfIncident()
                : (bc.getCreatedAt() != null ? bc.getCreatedAt().toLocalDate() : LocalDate.now());

        LocalTime incidentTime = (bc.getIncidentDetail() != null)
                ? bc.getIncidentDetail().getTimeOfIncident()
                : null;

        String place = (bc.getIncidentDetail() != null && bc.getIncidentDetail().getIncidentLocation() != null)
                ? bc.getIncidentDetail().getIncidentLocation()
                : "Barangay Hall";
        String nature = (bc.getIncidentDetail() != null && bc.getIncidentDetail().getNatureOfComplaint() != null)
                ? bc.getIncidentDetail().getNatureOfComplaint()
                : "Lost Document / Lost Certificate";

        String reason = nature + " (Blotter Case #" + bc.getCaseNumber() + ")";
        if (bc.getDocumentReissuanceRemarks() != null && !bc.getDocumentReissuanceRemarks().isBlank()) {
            reason += " - " + bc.getDocumentReissuanceRemarks();
        }

        String reviewedBy = bc.getDocumentReissuanceReviewedBy() != null
                ? formatUserFullName(bc.getDocumentReissuanceReviewedBy())
                : null;
        LocalDateTime reviewedAt = bc.getDocumentReissuanceReviewedAt();
        DocumentReissuanceStatus effectiveStatus = bc.getDocumentReissuanceStatus();
        if (effectiveStatus == null || effectiveStatus == DocumentReissuanceStatus.PENDING_VERIFICATION) {
            effectiveStatus = DocumentReissuanceStatus.APPROVED_FOR_REISSUANCE;
        }

        return new LinkedBlotterResponseDTO(
                bc.getId(),
                bc.getCaseNumber(),
                bc.getFilingType() != null ? bc.getFilingType().name() : "FOR_THE_RECORD",
                bc.getCaseStatus() != null ? bc.getCaseStatus().name() : "RECORDED",
                effectiveStatus.name(),
                bc.getDocumentReissuanceRemarks(),
                nature,
                incidentDate,
                incidentTime,
                place,
                reason,
                hasAffidavit,
                affidavitId,
                affidavitFileName,
                reviewedBy,
                reviewedAt,
                isAlreadyReissued
        );
    }

    /**
     * Creates a re-issuance request for an FTJS certificate subject to 1-year validity rules.
     */
    @Transactional
    public ReIssuanceRequestDto createReIssuanceRequest(Long ftjsId, ReIssuanceRequestDto request) throws BadRequestException {

        FirstTimeJobSeeker ftjs = firstTimeJobSeekerRepository.findById(ftjsId).orElseThrow(()
                -> new ResourceNotFoundException("ftjs not found"));

        LocalDate originalDate = ftjs.getDateSubmitted() != null ? ftjs.getDateSubmitted() : ftjs.getCreatedAt().toLocalDate();
        if (originalDate != null && LocalDate.now().isAfter(originalDate.plusDays(365))) {
            throw new BadRequestException("Cannot process re-issuance. The original FTJS certification has already expired (1-year validity limit under RA 11261).");
        }

        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null && ftjs.getVerifiedBy() != null) {
            currentUser = ftjs.getVerifiedBy();
        }
        if (currentUser == null) {
            currentUser = userRepository.findAll().stream()
                    .filter(u -> u.getPerson() != null)
                    .findFirst()
                    .orElse(null);
        }

        // 1. Resolve approved Blotter Case
        Case linkedBlotter = null;
        if (request.blotterCaseId() != null) {
            linkedBlotter = caseRepository.findById(request.blotterCaseId()).orElse(null);
        }
        if (linkedBlotter == null) {
            List<Case> approvedCases = caseRepository.findActiveDocumentReissuanceRequests(
                    ftjsId,
                    RelatedBarangayService.FTJS_REISSUANCE,
                    List.of(DocumentReissuanceStatus.APPROVED_FOR_REISSUANCE, DocumentReissuanceStatus.PENDING_VERIFICATION)
            );
            if (!approvedCases.isEmpty()) {
                linkedBlotter = approvedCases.get(0);
            }
        }

        if (linkedBlotter == null && (request.affidavit() == null || request.affidavit().length == 0)) {
            throw new BadRequestException("A cleared Blotter verification is required before this certificate can be reissued. Residents must report lost or damaged certificates to the Blotter Department.");
        }

        int currentCount = (ftjs.getIssuanceCount() != null) ? ftjs.getIssuanceCount() : 1;
        int nextIssuanceCount = currentCount + 1;

        ftjs.setIssuanceCount(nextIssuanceCount);
        ftjs.setStatus(FtjsStatus.RE_ISSUANCE);
        FirstTimeJobSeeker updatedFtjs = firstTimeJobSeekerRepository.save(ftjs);

        FirstTimeJobSeekerAffidavitOfLoss affidavit = new FirstTimeJobSeekerAffidavitOfLoss();
        affidavit.setFtjs(updatedFtjs);

        String resolvedReason = request.reason();
        LocalDate resolvedDateOfLoss = request.dateOfLoss();
        byte[] resolvedAffidavit = request.affidavit();

        if (linkedBlotter != null) {
            if (resolvedReason == null || resolvedReason.isBlank()) {
                String nature = linkedBlotter.getIncidentDetail() != null && linkedBlotter.getIncidentDetail().getNatureOfComplaint() != null
                        ? linkedBlotter.getIncidentDetail().getNatureOfComplaint()
                        : "Lost Document";
                resolvedReason = nature + " (Blotter Case #" + linkedBlotter.getCaseNumber() + ")";
                if (linkedBlotter.getDocumentReissuanceRemarks() != null && !linkedBlotter.getDocumentReissuanceRemarks().isBlank()) {
                    resolvedReason += " - " + linkedBlotter.getDocumentReissuanceRemarks();
                }
            }
            if (resolvedDateOfLoss == null && linkedBlotter.getIncidentDetail() != null) {
                resolvedDateOfLoss = linkedBlotter.getIncidentDetail().getDateOfIncident();
            }
            if (resolvedAffidavit == null || resolvedAffidavit.length == 0) {
                List<CaseEvidence> evidences = caseEvidenceRepository.findByCasesId(linkedBlotter.getId());
                CaseEvidence affidavitEv = evidences.stream()
                        .filter(er -> er.getFiles() != null && er.getFiles().length > 0)
                        .findFirst()
                        .orElse(null);
                if (affidavitEv != null) {
                    resolvedAffidavit = affidavitEv.getFiles();
                }
            }

            affidavit.setBlotterCase(linkedBlotter);
            linkedBlotter.setDocumentReissuanceStatus(DocumentReissuanceStatus.COMPLETED);
            linkedBlotter.setDocumentReissuanceRemarks("Re-issuance completed successfully. Replacement certificate issued under OR #" + request.orNumber());
            caseRepository.save(linkedBlotter);
        }

        if (resolvedDateOfLoss == null) {
            resolvedDateOfLoss = LocalDate.now();
        }
        if (resolvedReason == null || resolvedReason.isBlank()) {
            resolvedReason = "Lost Certificate Re-issuance";
        }

        affidavit.setReason(resolvedReason);
        affidavit.setDateOfLoss(resolvedDateOfLoss);
        affidavit.setOrNumber(request.orNumber());
        affidavit.setIssuanceNumber(nextIssuanceCount);
        affidavit.setCreatedBy(currentUser);
        affidavit.setAffidavitFiles(resolvedAffidavit);

        FirstTimeJobSeekerAffidavitOfLoss savedAffidavit = firstTimeJobSeekerAffidavitOfLossRepository.save(affidavit);

        FirstTimeJobSeekerTimeLine timeline = new FirstTimeJobSeekerTimeLine();
        timeline.setTimelineType(TimeLineType.RE_ISSUANCE);
        timeline.setTitle("Certificate Re-issued");
        timeline.setEventDate(LocalDateTime.now());
        timeline.setFtjs(updatedFtjs);
        timeline.setDescription("Re-issued FTJS Certificate (Copy #" + nextIssuanceCount + ") under OR #" + request.orNumber() +
                (linkedBlotter != null ? " [Blotter Case #" + linkedBlotter.getCaseNumber() + "]" : ""));
        timeline.setCreatedBy(currentUser);

        firstTimeJobSeekerTimeLineRepository.save(timeline);

        return mapToDto(savedAffidavit);
    }

    private ReIssuanceRequestDto mapToDto(FirstTimeJobSeekerAffidavitOfLoss affidavit) {
        return new ReIssuanceRequestDto(
                affidavit.getFtjs().getId(),
                affidavit.getReason(),
                affidavit.getDateOfLoss(),
                affidavit.getOrNumber(),
                affidavit.getAffidavitFiles(),
                affidavit.getBlotterCase() != null ? affidavit.getBlotterCase().getId() : null
        );
    }

    /**
     * Retrieves complete re-issuance history records for a given FTJS application.
     */
    @Transactional(readOnly = true)
    public List<ReIssuanceHistoryDto> getReIssuanceHistory(Long ftjsId) {
        if (!firstTimeJobSeekerRepository.existsById(ftjsId)) {
            throw new ResourceNotFoundException("FTJS record not found with id: " + ftjsId);
        }

        return firstTimeJobSeekerAffidavitOfLossRepository.findByFtjsIdOrderByIssuanceNumberDesc(ftjsId)
                .stream()
                .map(affidavit -> new ReIssuanceHistoryDto(
                        affidavit.getId(),
                        affidavit.getIssuanceNumber(),
                        affidavit.getDateOfLoss(),
                        affidavit.getReason(),
                        affidavit.getOrNumber(),
                        affidavit.getAffidavitFiles(),
                        affidavit.getCreatedAt(),
                        formatResidentFullName(affidavit.getFtjs()),
                        resolveIssuingOfficerName(affidavit)
                ))
                .toList();
    }

    private String formatResidentFullName(FirstTimeJobSeeker ftjs) {
        if (ftjs == null || ftjs.getResident() == null || ftjs.getResident().getPerson() == null) {
            return "N/A";
        }
        var person = ftjs.getResident().getPerson();
        StringBuilder sb = new StringBuilder();
        if (person.getFirstName() != null && !person.getFirstName().isBlank()) sb.append(person.getFirstName().trim());
        if (person.getMiddleName() != null && !person.getMiddleName().isBlank()) sb.append(" ").append(person.getMiddleName().trim());
        if (person.getLastName() != null && !person.getLastName().isBlank()) sb.append(" ").append(person.getLastName().trim());
        if (person.getSuffix() != null && !person.getSuffix().isBlank()) sb.append(" ").append(person.getSuffix().trim());
        return sb.toString().trim();
    }

    private String formatUserFullName(User user) {
        if (user == null) {
            return "System";
        }
        if (user.getPerson() == null) {
            return user.getUsername() != null && !user.getUsername().isBlank() ? user.getUsername() : "System";
        }
        StringBuilder sb = new StringBuilder();

        if (user.getPerson().getFirstName() != null && !user.getPerson().getFirstName().isBlank()) {
            sb.append(user.getPerson().getFirstName().trim());
        }
        if (user.getPerson().getMiddleName() != null && !user.getPerson().getMiddleName().isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(user.getPerson().getMiddleName().trim());
        }
        if (user.getPerson().getLastName() != null && !user.getPerson().getLastName().isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(user.getPerson().getLastName().trim());
        }
        if (user.getPerson().getSuffix() != null && !user.getPerson().getSuffix().isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(user.getPerson().getSuffix().trim());
        }

        return sb.isEmpty() ? (user.getUsername() != null ? user.getUsername() : "System") : sb.toString();
    }

    /**
     * Retrieves historical timeline audit events for a given FTJS application.
     */
    @Transactional(readOnly = true)
    public List<FtjsTimelineDto> getFtjsTimeline(Long ftjsId) {
        if (!firstTimeJobSeekerRepository.existsById(ftjsId)) {
            throw new ResourceNotFoundException("FTJS record not found with id: " + ftjsId);
        }

        return firstTimeJobSeekerTimeLineRepository.findByFtjsIdOrderByEventDateDesc(ftjsId)
                .stream()
                .map(timeline -> new FtjsTimelineDto(
                        timeline.getId(),
                        timeline.getTitle(),
                        timeline.getDescription(),
                        timeline.getTimelineType(),
                        timeline.getEventDate(),
                        formatUserFullName(timeline.getCreatedBy())
                ))
                .toList();
    }

    private String resolveIssuingOfficerName(FirstTimeJobSeekerAffidavitOfLoss affidavit) {
        if (affidavit == null) return "Barangay Official";
        
        String residentName = formatResidentFullName(affidavit.getFtjs());

        if (affidavit.getCreatedBy() != null) {
            String name = formatUserFullName(affidavit.getCreatedBy());
            if (name != null && !name.isBlank() && !"System".equalsIgnoreCase(name)) {
                if (!name.equalsIgnoreCase(residentName)) {
                    return name;
                }
            }
        }

        if (affidavit.getFtjs() != null && affidavit.getFtjs().getVerifiedBy() != null) {
            String name = formatUserFullName(affidavit.getFtjs().getVerifiedBy());
            if (name != null && !name.isBlank()) {
                if (!name.equalsIgnoreCase(residentName)) {
                    return name;
                }
            }
        }

        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            String name = formatUserFullName(currentUser);
            if (name != null && !name.isBlank() && !"System".equalsIgnoreCase(name) && !name.equalsIgnoreCase(residentName)) {
                return name;
            }
        }

        return "Barangay Official";
    }
}
