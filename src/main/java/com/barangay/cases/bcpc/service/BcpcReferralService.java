package com.barangay.cases.bcpc.service;

import com.barangay.cases.bcpc.dto.BcpcReferralCancelRequest;
import com.barangay.cases.bcpc.dto.BcpcReferralCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcReferralFeedbackDto;
import com.barangay.cases.bcpc.dto.BcpcReferralFeedbackRequest;
import com.barangay.cases.bcpc.dto.BcpcReferralResponse;
import com.barangay.cases.bcpc.model.BcpcReferral;
import com.barangay.cases.bcpc.model.BcpcReferralFeedback;
import com.barangay.cases.bcpc.repository.BcpcReferralFeedbackRepository;
import com.barangay.cases.bcpc.repository.BcpcReferralRepository;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.TimelineEventType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing external agency referrals and feedback endorsements for BCPC cases.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BcpcReferralService {

    private final CaseRepository caseRepository;
    private final BcpcReferralRepository bcpcReferralRepository;
    private final BcpcReferralFeedbackRepository bcpcReferralFeedbackRepository;
    private final CaseTimelineService caseTimelineService;

    @Transactional(readOnly = true)
    public List<BcpcReferralResponse> getReferrals(Long caseId) {
        if (!caseRepository.existsById(caseId)) {
            throw new ResourceNotFoundException("BCPC Case not found with ID: " + caseId);
        }
        return bcpcReferralRepository.findByCasesIdOrderByReferralDateDesc(caseId).stream()
                .map(this::toReferralResponse)
                .toList();
    }

    @Transactional
    public BcpcReferralResponse issueReferral(Long caseId, BcpcReferralCreateRequest request) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC Case not found with ID: " + caseId));

        User currentUser = SecurityUtils.getCurrentUser();
        String referredBy = request.getReferredBy();
        if (referredBy == null || referredBy.isBlank()) {
            referredBy = (currentUser != null && currentUser.getUsername() != null)
                    ? currentUser.getUsername()
                    : "BCPC Officer";
        }

        String controlNumber = generateReferralControlNumber(caseEntity);
        LocalDateTime referralDate = request.getReferralDate() != null
                ? request.getReferralDate()
                : LocalDateTime.now();

        BcpcReferral referral = new BcpcReferral();
        referral.setCases(caseEntity);
        referral.setReferralControlNumber(controlNumber);
        referral.setReferredTo(request.getReferredTo());
        referral.setReferredToOther(request.getReferredToOther());
        referral.setPurpose(request.getPurpose());
        referral.setPurposeOther(request.getPurposeOther());
        referral.setGrounds(request.getGrounds());
        referral.setReferredBy(referredBy);
        referral.setReferralDate(referralDate);
        referral.setStatus("Active");

        BcpcReferral saved = bcpcReferralRepository.save(referral);

        if (caseEntity.getCaseStatus() == CaseStatus.PENDING) {
            caseEntity.setCaseStatus(CaseStatus.ONGOING);
            caseRepository.save(caseEntity);
        }

        try {
            String targetAgency = (referral.getReferredToOther() != null && !referral.getReferredToOther().isBlank())
                    ? referral.getReferredToOther()
                    : referral.getReferredTo();
            String purpose = (referral.getPurposeOther() != null && !referral.getPurposeOther().isBlank())
                    ? referral.getPurposeOther()
                    : referral.getPurpose();

            String desc = "Issued referral to " + targetAgency + " for " + purpose + ".";

            caseTimelineService.logEvent(
                    caseEntity,
                    TimelineEventType.CASE_REFERRED,
                    "Inter-Agency Referral Issued (" + targetAgency + ")",
                    desc,
                    currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to log timeline event for referral issue [caseId={}]: {}", caseId, e.getMessage());
        }

        return toReferralResponse(saved);
    }

    @Transactional
    public BcpcReferralResponse recordReferralFeedback(Long caseId, Long referralId, BcpcReferralFeedbackRequest request) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC Case not found with ID: " + caseId));

        BcpcReferral referral = bcpcReferralRepository.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral not found with ID: " + referralId));

        if (!referral.getCases().getId().equals(caseId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Referral does not belong to case ID: " + caseId);
        }

        User currentUser = SecurityUtils.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime feedbackDate = request.getFeedbackDate() != null ? request.getFeedbackDate() : now;

        boolean isAccepted = Boolean.TRUE.equals(request.getAcceptedByAgency())
                || "Accepted".equalsIgnoreCase(request.getFeedbackResponse());
        boolean isDeclined = Boolean.TRUE.equals(request.getDeclinedByAgency())
                || "Declined".equalsIgnoreCase(request.getFeedbackResponse());

        String responseType = isAccepted ? "Accepted" : (isDeclined ? "Declined" : "Status Update");

        referral.setFeedbackResponse(responseType);
        referral.setFeedbackNotes(request.getDetails() != null ? request.getDetails() : request.getFeedbackNotes());
        referral.setFeedbackOfficer(request.getSocialWorkerName() != null ? request.getSocialWorkerName() : request.getFeedbackOfficer());
        referral.setFeedbackDate(feedbackDate);

        if (isAccepted) {
            referral.setStatus("Accepted");
        } else if (isDeclined) {
            referral.setStatus("Declined");
        }

        BcpcReferral savedReferral = bcpcReferralRepository.save(referral);

        BcpcReferralFeedback fb = new BcpcReferralFeedback();
        fb.setCases(caseEntity);
        fb.setReferral(savedReferral);
        fb.setDetails(request.getDetails() != null ? request.getDetails() : request.getFeedbackNotes());
        fb.setResolvesCase(Boolean.TRUE.equals(request.getIsResolved()));
        fb.setResolutionType(request.getResolutionType());
        fb.setProofFileData(request.getProofFileData());
        fb.setProofFileName(request.getProofFileName());
        fb.setDeclinedByAgency(isDeclined);
        fb.setAcceptedByAgency(isAccepted);
        fb.setReasonForDecline(request.getReasonForDecline());
        fb.setFeedbackTypeCode(request.getFeedbackTypeCode());
        fb.setFeedbackTypeOther(request.getFeedbackTypeOther());
        fb.setAssessmentFindings(request.getAssessmentFindings());
        fb.setAssessmentType(request.getAssessmentType());
        fb.setDiscernmentResult(request.getDiscernmentResult());
        fb.setAssessmentDate(request.getAssessmentDate());
        fb.setSocialWorkerName(request.getSocialWorkerName() != null ? request.getSocialWorkerName() : request.getFeedbackOfficer());
        fb.setRecommendationType(request.getRecommendationType());
        fb.setRecommendationTypeOther(request.getRecommendationTypeOther());
        fb.setRecommendedSocialWorker(request.getRecommendedSocialWorker());
        fb.setExternalRepresentativeName(request.getExternalRepresentativeName());
        fb.setExternalRepresentativeOffice(request.getExternalRepresentativeOffice());
        fb.setExternalRepresentativePosition(request.getExternalRepresentativePosition());
        fb.setExternalRepresentativeContact(request.getExternalRepresentativeContact());
        fb.setRecommendationDate(request.getRecommendationDate());
        fb.setRecommendedFamilySupport(request.getRecommendedFamilySupport());
        fb.setRecommendedPsychosocialAction(request.getRecommendedPsychosocialAction());

        if (request.getDateReceived() != null && !request.getDateReceived().isBlank()) {
            try {
                fb.setDateReceived(LocalDate.parse(request.getDateReceived()));
            } catch (Exception ignored) {
                fb.setDateReceived(LocalDate.now());
            }
        } else {
            fb.setDateReceived(LocalDate.now());
        }

        bcpcReferralFeedbackRepository.save(fb);

        if (caseEntity.getCaseStatus() == CaseStatus.PENDING) {
            caseEntity.setCaseStatus(CaseStatus.ONGOING);
            caseRepository.save(caseEntity);
        }

        try {
            String feedbackSummary = "Referral " + (Boolean.TRUE.equals(fb.getAcceptedByAgency()) ? "accepted" : Boolean.TRUE.equals(fb.getDeclinedByAgency()) ? "declined" : "status updated") + " by agency.";
            if (fb.getSocialWorkerName() != null && !fb.getSocialWorkerName().isBlank()) {
                feedbackSummary += " Assessed by " + fb.getSocialWorkerName() + ".";
            }

            caseTimelineService.logEvent(
                    caseEntity,
                    TimelineEventType.STATUS_CHANGED,
                    "Referral Feedback Received (" + referral.getReferralControlNumber() + ")",
                    feedbackSummary,
                    currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to log timeline event for referral feedback [caseId={}]: {}", caseId, e.getMessage());
        }

        return toReferralResponse(savedReferral);
    }

    @Transactional
    public BcpcReferralResponse cancelReferral(Long caseId, Long referralId, BcpcReferralCancelRequest request) {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC Case not found with ID: " + caseId));

        BcpcReferral referral = bcpcReferralRepository.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral not found with ID: " + referralId));

        if (!referral.getCases().getId().equals(caseId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Referral does not belong to case ID: " + caseId);
        }

        User currentUser = SecurityUtils.getCurrentUser();
        referral.setStatus("Cancelled");
        referral.setCancellationReason(request.getCancellationReason());

        String cancelledByName = "BCPC Officer";
        if (request.getCancelledBy() != null && !request.getCancelledBy().isBlank()) {
            cancelledByName = request.getCancelledBy().trim();
        } else if (currentUser != null) {
            if (currentUser.getPerson() != null) {
                String first = currentUser.getPerson().getFirstName() != null ? currentUser.getPerson().getFirstName() : "";
                String last = currentUser.getPerson().getLastName() != null ? currentUser.getPerson().getLastName() : "";
                String full = (first + " " + last).trim();
                cancelledByName = !full.isEmpty() ? full : currentUser.getUsername();
            } else {
                cancelledByName = currentUser.getUsername();
            }
        }
        referral.setCancelledBy(cancelledByName);
        referral.setCancelledAt(LocalDateTime.now());

        BcpcReferral saved = bcpcReferralRepository.save(referral);

        try {
            caseTimelineService.logEvent(
                    caseEntity,
                    TimelineEventType.STATUS_CHANGED,
                    "Referral Cancelled (" + referral.getReferralControlNumber() + ")",
                    "Referral was cancelled. Reason: " + request.getCancellationReason(),
                    currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to log timeline event for referral cancel [caseId={}]: {}", caseId, e.getMessage());
        }

        return toReferralResponse(saved);
    }

    private String generateReferralControlNumber(Case caseEntity) {
        long count = bcpcReferralRepository.countByCasesId(caseEntity.getId());
        String base = "REF-" + caseEntity.getCaseNumber() + "-" + String.format("%02d", count + 1);
        String candidate = base;
        int seq = 1;
        while (bcpcReferralRepository.existsByReferralControlNumber(candidate)) {
            candidate = base + "-" + seq++;
        }
        return candidate;
    }

    public BcpcReferralResponse toReferralResponse(BcpcReferral r) {
        List<BcpcReferralFeedbackDto> feedbackDtos = null;
        if (r.getId() != null) {
            feedbackDtos = bcpcReferralFeedbackRepository.findByReferralIdOrderByCreatedAtDesc(r.getId()).stream()
                    .map(this::toReferralFeedbackDto)
                    .toList();
        }

        return BcpcReferralResponse.builder()
                .id(r.getId())
                .caseId(r.getCases() != null ? r.getCases().getId() : null)
                .caseNumber(r.getCases() != null ? r.getCases().getCaseNumber() : null)
                .referralControlNumber(r.getReferralControlNumber())
                .referredTo(r.getReferredTo())
                .referredToOther(r.getReferredToOther())
                .purpose(r.getPurpose())
                .purposeOther(r.getPurposeOther())
                .grounds(r.getGrounds())
                .status(r.getStatus())
                .referredBy(r.getReferredBy())
                .referralDate(r.getReferralDate())
                .feedbackResponse(r.getFeedbackResponse())
                .feedbackNotes(r.getFeedbackNotes())
                .feedbackOfficer(r.getFeedbackOfficer())
                .feedbackDate(r.getFeedbackDate())
                .feedbacks(feedbackDtos)
                .cancellationReason(r.getCancellationReason())
                .cancelledBy(r.getCancelledBy())
                .cancelledAt(r.getCancelledAt())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private BcpcReferralFeedbackDto toReferralFeedbackDto(BcpcReferralFeedback fb) {
        return BcpcReferralFeedbackDto.builder()
                .id(fb.getId())
                .caseId(fb.getCases() != null ? fb.getCases().getId() : null)
                .referralId(fb.getReferral() != null ? fb.getReferral().getId() : null)
                .details(fb.getDetails())
                .resolvesCase(fb.getResolvesCase())
                .resolutionType(fb.getResolutionType())
                .proofFileData(fb.getProofFileData())
                .proofFileName(fb.getProofFileName())
                .declinedByAgency(fb.getDeclinedByAgency())
                .acceptedByAgency(fb.getAcceptedByAgency())
                .reasonForDecline(fb.getReasonForDecline())
                .feedbackTypeCode(fb.getFeedbackTypeCode())
                .feedbackTypeOther(fb.getFeedbackTypeOther())
                .assessmentFindings(fb.getAssessmentFindings())
                .assessmentType(fb.getAssessmentType())
                .discernmentResult(fb.getDiscernmentResult())
                .assessmentDate(fb.getAssessmentDate())
                .socialWorkerName(fb.getSocialWorkerName())
                .recommendationType(fb.getRecommendationType())
                .recommendationTypeOther(fb.getRecommendationTypeOther())
                .recommendedSocialWorker(fb.getRecommendedSocialWorker())
                .externalRepresentativeName(fb.getExternalRepresentativeName())
                .externalRepresentativeOffice(fb.getExternalRepresentativeOffice())
                .externalRepresentativePosition(fb.getExternalRepresentativePosition())
                .externalRepresentativeContact(fb.getExternalRepresentativeContact())
                .recommendationDate(fb.getRecommendationDate())
                .recommendedFamilySupport(fb.getRecommendedFamilySupport())
                .recommendedPsychosocialAction(fb.getRecommendedPsychosocialAction())
                .dateReceived(fb.getDateReceived())
                .createdAt(fb.getCreatedAt())
                .build();
    }
}
