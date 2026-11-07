package com.barangay.cases.bcpc.service;

import com.barangay.cases.bcpc.dto.BcpcCustodyLogDto;
import com.barangay.cases.bcpc.dto.UpdateCiclCustodyLogDto;
import com.barangay.cases.bcpc.model.BcpcCaseDetails;
import com.barangay.cases.bcpc.repository.BcpcCaseDetailsRepository;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.enumerated.BcpcCategory;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.TimelineEventType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.security.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service managing Custody & Turnover records under BCPC (for both CAR and CICL).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BcpcCustodyService {

    private final CaseRepository caseRepository;
    private final BcpcCaseDetailsRepository bcpcCaseDetailsRepository;
    private final CaseTimelineService caseTimelineService;

    @Transactional(readOnly = true)
    public BcpcCustodyLogDto getCustodyLog(Long caseId) {
        log.info("Retrieving Custody Log for BCPC Case ID: {}", caseId);
        caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC Case not found with ID: " + caseId));
        BcpcCaseDetails details = bcpcCaseDetailsRepository.findByCasesId(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC Details not found for Case ID: " + caseId));

        return toCustodyLogDto(details);
    }

    public BcpcCustodyLogDto toCustodyLogDto(BcpcCaseDetails details) {
        String custodyStatus = details.getCustodyStatus();
        if (custodyStatus == null || custodyStatus.isBlank()) {
            if (details.getTemporaryPlacement() != null && !details.getTemporaryPlacement().isBlank()) {
                if (CarTurnoverPolicy.TEMPORARY.equals(details.getTemporaryPlacement())) {
                    custodyStatus = "Temporarily Accommodated";
                } else {
                    custodyStatus = "Turnover Completed";
                }
            } else if (details.getWasApprehended() != null) {
                if (Boolean.FALSE.equals(details.getWasApprehended())) {
                    custodyStatus = "Not in Custody";
                } else if (details.getCustodyResolution() != null && !details.getCustodyResolution().isBlank() && !"Pending Resolution".equalsIgnoreCase(details.getCustodyResolution())) {
                    custodyStatus = "Turnover Completed";
                } else {
                    custodyStatus = "In Custody - Awaiting Turnover";
                }
            } else {
                custodyStatus = "Pending Turnover";
            }
        }

        JsonNode receiverNode = CarTurnoverPolicy.receiver(details.getInitialResponsibility());
        String fn = CarTurnoverPolicy.text(receiverNode, "firstName");
        String mn = CarTurnoverPolicy.text(receiverNode, "middleName");
        String ln = CarTurnoverPolicy.text(receiverNode, "lastName");
        String receivingPersonName = (fn + " " + mn + " " + ln).trim().replaceAll(" +", " ");
        if (receivingPersonName.isEmpty()) receivingPersonName = fn;

        String receivingRelationship = CarTurnoverPolicy.text(receiverNode, "relationship");
        if (CarTurnoverPolicy.TEMPORARY.equals(details.getTemporaryPlacement())) {
            receivingRelationship = CarTurnoverPolicy.text(receiverNode, "officePosition");
            if (receivingRelationship.isEmpty()) receivingRelationship = "Barangay Personnel";
        }

        String receivingOffice = CarTurnoverPolicy.text(receiverNode, "receivingOffice");
        if (receivingOffice.isEmpty()) receivingOffice = CarTurnoverPolicy.text(receiverNode, "receivingBarangay");

        String immediateActionNotes = CarTurnoverPolicy.text(receiverNode, "immediateActionNotes");

        boolean carGuidanceApplicable = CarTurnoverPolicy.PARENT.equals(details.getTemporaryPlacement()) ||
                (details.getCategory() == BcpcCategory.CAR && details.getTemporaryPlacement() == null);

        String apprehOfficer = null;
        String apprehPlace = null;
        String apprehDate = null;
        String apprehTime = null;
        if (details.getCiclAssessment() != null) {
            apprehOfficer = details.getCiclAssessment().getApprehendingOfficerName();
            apprehPlace = details.getCiclAssessment().getApprehensionPlace();
            if (details.getCiclAssessment().getApprehensionDateTime() != null) {
                apprehDate = details.getCiclAssessment().getApprehensionDateTime().toLocalDate().toString();
                apprehTime = details.getCiclAssessment().getApprehensionDateTime().toLocalTime().toString();
            }
        }

        return BcpcCustodyLogDto.builder()
                .turnoverAction("NEW_TURNOVER")
                .custodyStatus(custodyStatus)
                .needsMedicalAttention(details.getNeedsMedicalAttention())
                .temporaryPlacement(details.getTemporaryPlacement() != null ? details.getTemporaryPlacement() : details.getCustodyResolution())
                .initialResponsibility(details.getInitialResponsibility())
                .contactNumber(details.getContactNumber())
                .transferDate(details.getTransferDate())
                .transferTime(details.getTransferTime())
                .temporaryPlacementReason(details.getTemporaryPlacementReason() != null ? details.getTemporaryPlacementReason() : details.getDelayReason())
                .transferNotes(details.getTransferNotes() != null ? details.getTransferNotes() : details.getCustodyRemarks())
                .healthCoordinationConfirmed(details.getHealthCoordinationConfirmed())
                .childGuidanceProvided(details.getChildGuidanceProvided())
                .parentAdviceProvided(details.getParentAdviceProvided())
                .guidanceNotes(details.getGuidanceNotes())
                .receivingPersonName(receivingPersonName)
                .receivingRelationship(receivingRelationship)
                .receivingOffice(receivingOffice)
                .immediateActionNotes(immediateActionNotes)
                .carGuidanceApplicable(carGuidanceApplicable)
                .wasApprehended(details.getWasApprehended())
                .apprehendingOfficerName(apprehOfficer)
                .apprehensionPlace(apprehPlace)
                .apprehensionDate(apprehDate)
                .apprehensionTime(apprehTime)
                .custodyResolution(details.getCustodyResolution())
                .turnoverDate(details.getTurnoverDate())
                .turnoverTime(details.getTurnoverTime())
                .delayReason(details.getDelayReason())
                .custodyRemarks(details.getCustodyRemarks())
                .build();
    }

    @Transactional
    public BcpcCustodyLogDto updateCustodyLog(Long caseId, BcpcCustodyLogDto request) {
        log.info("Updating Custody Log for BCPC Case ID: {}", caseId);
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC Case not found with ID: " + caseId));
        if (caseEntity.getCaseStatus() == CaseStatus.SETTLED || caseEntity.getCaseStatus() == CaseStatus.DISMISSED || caseEntity.getCaseStatus() == CaseStatus.WITHDRAWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update custody for settled, dismissed, or withdrawn cases.");
        }

        BcpcCaseDetails details = bcpcCaseDetailsRepository.findByCasesId(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC Details not found for Case ID: " + caseId));

        User currentUser = SecurityUtils.getCurrentUser();

        details.setTemporaryPlacement(request.getTemporaryPlacement());
        details.setTemporaryPlacementReason(request.getTemporaryPlacementReason());
        details.setContactNumber(request.getContactNumber());
        details.setTransferDate(request.getTransferDate());
        details.setTransferTime(request.getTransferTime());
        details.setNeedsMedicalAttention(request.getNeedsMedicalAttention());
        details.setHealthCoordinationConfirmed(request.getHealthCoordinationConfirmed());
        details.setTransferNotes(request.getTransferNotes());
        details.setChildGuidanceProvided(request.getChildGuidanceProvided());
        details.setParentAdviceProvided(request.getParentAdviceProvided());
        details.setGuidanceNotes(request.getGuidanceNotes());

        if (CarTurnoverPolicy.TEMPORARY.equals(request.getTemporaryPlacement())) {
            details.setCustodyStatus("Temporarily Accommodated");
        } else {
            details.setCustodyStatus("Turnover Completed");
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode receiverNode = mapper.createObjectNode();
        if (request.getReceivingPersonName() != null) receiverNode.put("firstName", request.getReceivingPersonName().trim());
        if (request.getReceivingRelationship() != null) receiverNode.put("relationship", request.getReceivingRelationship().trim());
        if (request.getReceivingOffice() != null) receiverNode.put("receivingOffice", request.getReceivingOffice().trim());
        if (request.getImmediateActionNotes() != null) receiverNode.put("immediateActionNotes", request.getImmediateActionNotes().trim());
        details.setInitialResponsibility(receiverNode.toString());

        bcpcCaseDetailsRepository.save(details);

        try {
            caseTimelineService.logEvent(
                    caseEntity,
                    TimelineEventType.STATUS_CHANGED,
                    "Custody Handoff Recorded",
                    "Custody handoff record updated. Handoff to: " + request.getTemporaryPlacement(),
                    currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to log custody timeline event: {}", e.getMessage());
        }

        return toCustodyLogDto(details);
    }

    @Transactional
    public BcpcCustodyLogDto updateCiclCustodyLog(Long caseId, UpdateCiclCustodyLogDto request) {
        log.info("Updating CICL Custody Log for BCPC Case ID: {}", caseId);
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC Case not found with ID: " + caseId));
        if (caseEntity.getCaseStatus() == CaseStatus.SETTLED || caseEntity.getCaseStatus() == CaseStatus.DISMISSED || caseEntity.getCaseStatus() == CaseStatus.WITHDRAWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update custody for settled, dismissed, or withdrawn cases.");
        }

        BcpcCaseDetails details = bcpcCaseDetailsRepository.findByCasesId(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC Details not found for Case ID: " + caseId));

        User currentUser = SecurityUtils.getCurrentUser();

        if (Boolean.FALSE.equals(request.getWasApprehended())) {
            details.setWasApprehended(false);
            details.setCustodyStatus("Not in Custody");
            bcpcCaseDetailsRepository.save(details);
            return toCustodyLogDto(details);
        }

        details.setWasApprehended(true);
        if (request.getCustodyResolution() != null && !request.getCustodyResolution().isBlank()
                && !"Pending Resolution".equalsIgnoreCase(request.getCustodyResolution())) {
            details.setCustodyResolution(request.getCustodyResolution());
            details.setTurnoverDate(request.getTurnoverDate());
            details.setTurnoverTime(request.getTurnoverTime());
            details.setTurnedOverTo(request.getTurnedOverTo());
            details.setTurnedOverToSpecify(request.getTurnedOverToSpecify());
            details.setDelayReason(request.getDelayReason());
            details.setCustodyRemarks(request.getCustodyRemarks());
            details.setCustodyStatus("Turnover Completed");
            details.setContactNumber(request.getContactNumber());
            details.setTemporaryPlacement(request.getCustodyResolution());
            if (request.getDelayReason() != null && !request.getDelayReason().isBlank()) {
                details.setTemporaryPlacementReason(request.getDelayReason());
            }
            if (request.getCustodyRemarks() != null && !request.getCustodyRemarks().isBlank()) {
                details.setTransferNotes(request.getCustodyRemarks());
            }

            String receiverName = request.getReleasedTo() != null ? request.getReleasedTo().trim() : "";
            String receiverRel = request.getRecipientRelationship() != null ? request.getRecipientRelationship().trim() : "";
            if (!receiverName.isEmpty() || !receiverRel.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode node = mapper.createObjectNode();
                node.put("firstName", receiverName);
                node.put("representativeName", receiverName);
                node.put("relationship", receiverRel);
                try {
                    details.setInitialResponsibility(mapper.writeValueAsString(node));
                } catch (Exception ignored) {
                    details.setInitialResponsibility("{\"firstName\":\"" + receiverName + "\",\"relationship\":\"" + receiverRel + "\"}");
                }
            }

            bcpcCaseDetailsRepository.save(details);

            String receiver = request.getReleasedTo() != null ? request.getReleasedTo() : request.getTurnedOverTo();
            String rel = request.getRecipientRelationship();
            String desc = "Child turned over" + (receiver != null && !receiver.isBlank() ? " to " + receiver : "") + (rel != null && !rel.isBlank() ? " (" + rel + ")" : "") + ".";

            try {
                caseTimelineService.logEvent(
                        caseEntity,
                        TimelineEventType.STATUS_CHANGED,
                        "Custody / Turnover Completed",
                        desc,
                        currentUser
                );
            } catch (Exception e) {
                log.warn("Failed to log custody timeline event: {}", e.getMessage());
            }
        }

        return toCustodyLogDto(details);
    }
}
