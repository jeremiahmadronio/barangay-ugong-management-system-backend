package com.barangay.cases.blotter.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.barangay.cases.blotter.dto.*;
import com.barangay.zshared.exception.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.CaseEvidence;
import com.barangay.cases.shared.model.IncidentDetail;
import com.barangay.cases.shared.model.NarrativeStatement;
import com.barangay.cases.shared.repository.CaseEvidenceRepository;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.people.witness.model.Witness;
import com.barangay.people.witness.repository.WitnessRepository;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.dto.ComplainantDetailDto;
import com.barangay.zshared.dto.EvidenceCreateDto;
import com.barangay.zshared.dto.EvidenceDetailDto;
import com.barangay.zshared.dto.IncidentDetailDto;
import com.barangay.zshared.dto.NarrativeDetailDto;
import com.barangay.zshared.dto.RespondentDetailDto;
import com.barangay.zshared.dto.WitnessCreateDto;
import com.barangay.zshared.dto.WitnessDetailDto;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import com.barangay.zshared.enumerated.TimelineEventType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.mapper.CaseEvidenceMapper;
import com.barangay.zshared.mapper.ComplainantMapper;
import com.barangay.zshared.mapper.IncidentDetailMapper;
import com.barangay.zshared.mapper.NarrativeStatementMapper;
import com.barangay.zshared.mapper.PageResponse;
import com.barangay.zshared.mapper.RespondentMapper;
import com.barangay.zshared.mapper.WitnessMapper;
import com.barangay.zshared.security.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing Blotter Case CRUD operations, KPI statistics, case timeline events, and case number generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlotterCaseService {

    private final CaseRepository caseRepository;
    private final PersonRepository personRepository;
    private final OfficerRepository officerRepository;
    private final DepartmentRepository departmentRepository;
    private final CaseEvidenceRepository caseEvidenceRepository;
    private final WitnessRepository witnessRepository;
    private final CaseTimelineService caseTimelineService;

    private final ComplainantMapper complainantMapper;
    private final RespondentMapper respondentMapper;
    private final IncidentDetailMapper incidentDetailMapper;
    private final NarrativeStatementMapper narrativeStatementMapper;
    private final CaseEvidenceMapper caseEvidenceMapper;
    private final WitnessMapper witnessMapper;

    /**
     * Retrieves paginated and filtered Blotter cases without using findAll().
     *
     * @param filter the filtering criteria and pagination params
     * @return {@link PageResponse} of {@link BlotterCaseDetailResponse}
     */
    @Transactional(readOnly = true)
    public PageResponse<BlotterTableRowResponse> getBlotterCases(BlotterFilterRequest filter) {
        int page = filter != null ? filter.getPageNumber() : 0;
        int size = filter != null ? filter.getPageSize() : 10;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Case> casePage = caseRepository.findBlotterCasesFiltered(filter, pageable);

        Page<BlotterTableRowResponse> dtoPage = casePage.map(this::toTableRowResponse);
        return PageResponse.of(dtoPage);
    }

    /**
     * Maps a root {@link Case} entity to {@link BlotterTableRowResponse} for the Blotter Records Table.
     * Traverses the reOpenedCaseNumber chain to find the latest active child case, effective status, and cycle count.
     */
    private BlotterTableRowResponse toTableRowResponse(Case rootCase) {
        if (rootCase == null) return null;

        Case trueRoot = rootCase;
        while (trueRoot.getPreviousCaseNumber() != null && !trueRoot.getPreviousCaseNumber().trim().isEmpty()) {
            java.util.Optional<Case> prevOpt = caseRepository.findByCaseNumber(trueRoot.getPreviousCaseNumber().trim());
            if (prevOpt.isPresent()) {
                trueRoot = prevOpt.get();
            } else {
                break;
            }
        }

        List<Case> lineage = new ArrayList<>();
        Case curr = trueRoot;
        lineage.add(curr);
        while (curr.getReOpenedCaseNumber() != null && !curr.getReOpenedCaseNumber().trim().isEmpty()) {
            java.util.Optional<Case> nextOpt = caseRepository.findByCaseNumber(curr.getReOpenedCaseNumber().trim());
            if (nextOpt.isPresent()) {
                curr = nextOpt.get();
                lineage.add(curr);
            } else {
                break;
            }
        }

        Case latestCase = lineage.get(lineage.size() - 1);
        int cycleCount = lineage.size();
        boolean isReopened = cycleCount > 1;

        String filingTypeLabel = trueRoot.getFilingType() != null ?
                (trueRoot.getFilingType() == FilingType.FORMAL_COMPLAINT ? "Formal Complaint" : "For the Record") :
                "Blotter Entry";

        String compName = "—";
        Complainant comp = latestCase.getComplainant() != null ? latestCase.getComplainant() : trueRoot.getComplainant();
        if (comp != null && comp.getPerson() != null) {
            String f = comp.getPerson().getFirstName() != null ? comp.getPerson().getFirstName() : "";
            String l = comp.getPerson().getLastName() != null ? comp.getPerson().getLastName() : "";
            String m = comp.getPerson().getMiddleName() != null ? comp.getPerson().getMiddleName() : "";
            compName = (l + (l.isBlank() || f.isBlank() ? "" : ", ") + f + (m.isBlank() ? "" : " " + m.charAt(0) + ".")).trim();
        }

        String respName = "—";
        Respondent resp = latestCase.getRespondent() != null ? latestCase.getRespondent() : trueRoot.getRespondent();
        if (resp != null && resp.getPerson() != null) {
            String f = resp.getPerson().getFirstName() != null ? resp.getPerson().getFirstName() : "";
            String l = resp.getPerson().getLastName() != null ? resp.getPerson().getLastName() : "";
            String m = resp.getPerson().getMiddleName() != null ? resp.getPerson().getMiddleName() : "";
            respName = (l + (l.isBlank() || f.isBlank() ? "" : ", ") + f + (m.isBlank() ? "" : " " + m.charAt(0) + ".")).trim();
        }

        LocalDateTime dateFiled = trueRoot.getCreatedAt() != null ? trueRoot.getCreatedAt() : LocalDateTime.now();

        return new BlotterTableRowResponse(
                trueRoot.getId(),
                trueRoot.getCaseNumber(),
                latestCase.getId(),
                latestCase.getCaseNumber(),
                filingTypeLabel,
                cycleCount,
                isReopened,
                latestCase.getCaseStatus(),
                compName,
                respName,
                dateFiled
        );
    }

    /**
     * Retrieves single blotter case details by ID including full complainant, respondent,
     * incident details, narrative statement, evidence, witnesses, and officer assignments.
     *
     * @param id the case ID
     * @return {@link BlotterCaseDetailResponse} containing all case sub-entities
     */
    @Transactional(readOnly = true)
    public BlotterCaseDetailResponse getBlotterCaseById(Long id) {
        Case aCase = caseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blotter case not found with id: " + id));

        return toDetailResponse(aCase, null, null);
    }

    /**
     * Calculates Blotter KPI Statistics with month-to-date vs full previous calendar month trend percentages.
     *
     * @return {@link BlotterStatsResponse}
     */
    @Transactional(readOnly = true)
    public BlotterStatsResponse getBlotterStats() {
        LocalDateTime currentMonthStart = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime prevMonthStart = YearMonth.now().minusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime prevMonthEnd = currentMonthStart.minusNanos(1);

        // 1. Total Cases (Root Cases Only)
        long totalCases = caseRepository.countRootCases();
        long currentTotal = caseRepository.countRootCasesByCreatedAtGreaterThanEqual(currentMonthStart);
        long prevTotal = caseRepository.countRootCasesByCreatedAtBetween(prevMonthStart, prevMonthEnd);
        String totalCasesTrend = calculateMonthlyTrend(currentTotal, prevTotal);

        // 2. Total For the Record (Root Cases Only)
        long totalForTheRecord = caseRepository.countRootCasesByFilingType(FilingType.FOR_RECORD_ONLY);
        long currentForRecord = caseRepository.countRootCasesByFilingTypeAndCreatedAtGreaterThanEqual(FilingType.FOR_RECORD_ONLY, currentMonthStart);
        long prevForRecord = caseRepository.countRootCasesByFilingTypeAndCreatedAtBetween(FilingType.FOR_RECORD_ONLY, prevMonthStart, prevMonthEnd);
        String totalForTheRecordTrend = calculateMonthlyTrend(currentForRecord, prevForRecord);

        // 3. Under Mediation / In Progress (Latest Active Cycles Only)
        List<CaseStatus> activeStatuses = List.of(CaseStatus.UNDER_MEDIATION, CaseStatus.UNDER_CONCILIATION);
        long inProgressCases = caseRepository.countLatestCasesByCaseStatusIn(activeStatuses);

        // 4. Settled / Resolved Cases (Latest Active Cycles Only)
        List<CaseStatus> resolvedStatuses = List.of(CaseStatus.SETTLED);
        long resolvedCases = caseRepository.countLatestCasesByCaseStatusIn(resolvedStatuses);
        long currentResolved = caseRepository.countLatestCasesByCaseStatusInAndCreatedAtGreaterThanEqual(resolvedStatuses, currentMonthStart);
        long prevResolved = caseRepository.countLatestCasesByCaseStatusInAndCreatedAtBetween(resolvedStatuses, prevMonthStart, prevMonthEnd);
        String resolvedCasesTrend = calculateMonthlyTrend(currentResolved, prevResolved);

        return new BlotterStatsResponse(
                totalCases,
                totalCasesTrend,
                totalForTheRecord,
                totalForTheRecordTrend,
                inProgressCases,
                resolvedCases,
                resolvedCasesTrend
        );
    }

    /**
     * Creates a new Blotter Case (Formal Complaint or For the Record).
     *
     * @param request the case creation payload containing complainant, respondent, incident details, narrative, and witnesses
     * @return the created case details mapped to {@link BlotterCaseDetailResponse}
     */
    @Transactional
    public BlotterCaseDetailResponse createBlotterCase(BlotterCaseCreateRequest request) {
        Case aCase = new Case();

        // 1. Filing Type & Unique Case Number & Status
        FilingType filingType = request.filingType() != null ? request.filingType() : FilingType.FORMAL_COMPLAINT;
        aCase.setFilingType(filingType);

        String caseNumber = request.caseNumber();
        if (caseNumber == null || caseNumber.isBlank() || caseRepository.existsByCaseNumber(caseNumber.trim())) {
            caseNumber = generateUniqueCaseNumber();
        }
        aCase.setCaseNumber(caseNumber);
        if (filingType == FilingType.FOR_RECORD_ONLY) {
            aCase.setCaseStatus(CaseStatus.RECORDED);
        } else {
            aCase.setCaseStatus(CaseStatus.PENDING);
        }
        aCase.setIsArchived(false);
        aCase.setIsAttested(request.isCertified() != null ? request.isCertified() : false);

        // Auto-assign Blotter Department
        departmentRepository.findByName("BLOTTER").ifPresent(aCase::setDepartment);

        // Auto-assign caseFiledBy from logged-in security context
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            aCase.setCaseFilledBy(currentUser);
        }

        // 2. Complainant
        if (request.complainant() != null) {
            Person compPerson = complainantMapper.toNewPerson(request.complainant());
            if (compPerson != null) {
                compPerson = personRepository.save(compPerson);
                Complainant complainant = complainantMapper.toComplainant(compPerson);
                aCase.setComplainant(complainant);
            }
        }

        // 3. Respondent
        if (request.respondent() != null) {
            Person respPerson = respondentMapper.toNewPerson(request.respondent());
            if (respPerson != null) {
                respPerson = personRepository.save(respPerson);
                Respondent respondent = respondentMapper.toRespondent(respPerson, request.respondent());
                aCase.setRespondent(respondent);
            }
        }

        // 4. Incident Detail
        if (request.incident() != null) {
            IncidentDetail incidentDetail = incidentDetailMapper.fromCreateDto(request.incident());
            aCase.setIncidentDetail(incidentDetail);
        }

        // 5. Narrative Statement
        if (request.narrative() != null) {
            NarrativeStatement narrativeStatement = narrativeStatementMapper.fromCreateDto(request.narrative());
            aCase.setNarrativeStatement(narrativeStatement);
        }

        // 6. Assigned Officer lookup (case_assign_to)
        if (request.assignedOfficerId() != null) {
            officerRepository.findById(request.assignedOfficerId()).ifPresent(aCase::setOfficer);
        } else if (request.assignedOfficer() != null && !request.assignedOfficer().isBlank()) {
            // NOTE: Inefficient and fragile officer lookup using officerRepository.findAll() + stream filter.
            // Recommended to replace with a dedicated repository query (e.g. searching by name/person in DB).
            String searchName = request.assignedOfficer().toLowerCase();
            officerRepository.findAll().stream()
                    .filter(o -> o.getPerson() != null && (
                            (o.getPerson().getFirstName() + " " + o.getPerson().getLastName()).toLowerCase().contains(searchName) ||
                                    searchName.contains(o.getPerson().getLastName().toLowerCase())
                    ))
                    .findFirst()
                    .ifPresent(aCase::setOfficer);
        }

        // 7. Initial Save Case Entity
        Case savedCase = caseRepository.save(aCase);

        // 8. Witnesses
        if (request.witnesses() != null && !request.witnesses().isEmpty()) {
            List<Witness> witnessEntities = new ArrayList<>();
            for (WitnessCreateDto wDto : request.witnesses()) {
                Person wPerson = witnessMapper.toNewPerson(wDto);
                if (wPerson == null) {
                    wPerson = new Person();
                    wPerson.setFirstName("Witness");
                    wPerson.setLastName("Statement");
                }
                wPerson = personRepository.save(wPerson);
                Witness witness = witnessMapper.toWitness(wPerson, savedCase, wDto);
                witness = witnessRepository.save(witness);
                witnessEntities.add(witness);
            }
            savedCase.setWitnesses(witnessEntities);
            savedCase = caseRepository.save(savedCase);
        }

        // 9. Evidences (Save to CaseEvidence table)
        if (request.evidences() != null && !request.evidences().isEmpty()) {
            for (EvidenceCreateDto eDto : request.evidences()) {
                CaseEvidence evidence = caseEvidenceMapper.fromCreateDto(eDto, savedCase);
                if (evidence != null) {
                    caseEvidenceRepository.save(evidence);
                }
            }
        } else if (request.evidence() != null) {
            CaseEvidence evidence = caseEvidenceMapper.fromCreateDto(request.evidence(), savedCase);
            if (evidence != null) {
                caseEvidenceRepository.save(evidence);
            }
        }

        // 10. Map to Response DTO
        BlotterCaseDetailResponse response = toDetailResponse(savedCase, request.receivingOfficer(), request.assignedOfficer());

        // 11. Log Timeline Event
        try {
            caseTimelineService.logEvent(
                    savedCase,
                    TimelineEventType.CASE_FILED,
                    "Case Created",
                    "Blotter case record intake created.",
                    SecurityUtils.getCurrentUser()
            );
        } catch (Exception e) {
            log.warn("Failed to log timeline event for case creation [caseId={}]: {}", savedCase.getId(), e.getMessage(), e);
        }

        return response;
    }

    /**
     * Retrieves all timeline events for a case.
     *
     * @param caseId the blotter case ID
     * @return list of {@link TimelineEventResponse}
     */
    @Transactional(readOnly = true)
    public List<TimelineEventResponse> getCaseTimeline(Long caseId) {
        return caseTimelineService.getTimelineForCase(caseId);
    }

    /**
     * Formats month-to-date count vs full previous month count trend.
     */
    private String calculateMonthlyTrend(long currentCount, long prevCount) {
        if (prevCount == 0) {
            if (currentCount > 0) return "+100.0% vs last month";
            return "0.0% vs last month";
        }
        double percentChange = ((double) (currentCount - prevCount) / prevCount) * 100.0;
        return String.format("%+.1f%% vs last month", percentChange);
    }

    /**
     * Generates a guaranteed unique case number checked against the database.
     * Format: BC-YYYYMMDD-XXXX (e.g. BC-20260805-0001)
     */
    private String generateUniqueCaseNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int sequence = 1;
        String candidate;
        do {
            candidate = String.format("BC-%s-%04d", datePart, sequence++);
        } while (caseRepository.existsByCaseNumber(candidate));
        return candidate;
    }

    /**
     * Traverses the case lineage (both backwards via previousCaseNumber and forwards via reOpenedCaseNumber)
     * to return all cases belonging to the same case family.
     */
    private List<CaseHistoryItemResponse> buildCaseHistory(Case currentCase) {
        if (currentCase == null) return List.of();

        // 1. Find root case
        Case rootCase = currentCase;
        while (rootCase.getPreviousCaseNumber() != null && !rootCase.getPreviousCaseNumber().trim().isEmpty()) {
            java.util.Optional<Case> prevOpt = caseRepository.findByCaseNumber(rootCase.getPreviousCaseNumber().trim());
            if (prevOpt.isPresent()) {
                rootCase = prevOpt.get();
            } else {
                break;
            }
        }

        // 2. Traverse forwards from root case
        List<Case> lineage = new ArrayList<>();
        Case curr = rootCase;
        lineage.add(curr);
        while (curr.getReOpenedCaseNumber() != null && !curr.getReOpenedCaseNumber().trim().isEmpty()) {
            java.util.Optional<Case> nextOpt = caseRepository.findByCaseNumber(curr.getReOpenedCaseNumber().trim());
            if (nextOpt.isPresent()) {
                curr = nextOpt.get();
                lineage.add(curr);
            } else {
                break;
            }
        }

        // 3. Map lineage to DTOs
        Case latestCase = lineage.get(lineage.size() - 1);
        List<CaseHistoryItemResponse> historyList = new ArrayList<>();
        for (Case c : lineage) {
            boolean isCurrent = c.getId().equals(currentCase.getId());
            boolean isLatestActive = c.getId().equals(latestCase.getId());
            boolean isPast = !isLatestActive;

            String formattedDate = c.getCreatedAt() != null ?
                    c.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy")) : "";

            historyList.add(new CaseHistoryItemResponse(
                    c.getId(),
                    c.getCaseNumber(),
                    formattedDate,
                    c.getCaseStatus(),
                    isCurrent,
                    isPast,
                    isLatestActive
            ));
        }
        return historyList;
    }

    /**
     * Maps a saved {@link Case} entity to {@link BlotterCaseDetailResponse}.
     */
    private BlotterCaseDetailResponse toDetailResponse(Case aCase, String receivingOfficerName, String assignedOfficerName) {
        // Find root case for deadline calculations and missing intake details fallback
        Case rootCase = aCase;
        while (rootCase.getPreviousCaseNumber() != null && !rootCase.getPreviousCaseNumber().trim().isEmpty()) {
            java.util.Optional<Case> prevOpt = caseRepository.findByCaseNumber(rootCase.getPreviousCaseNumber().trim());
            if (prevOpt.isPresent()) {
                rootCase = prevOpt.get();
            } else {
                break;
            }
        }

        ComplainantDetailDto complainantDto = complainantMapper.toDetailDto(aCase.getComplainant() != null ? aCase.getComplainant() : rootCase.getComplainant());
        RespondentDetailDto respondentDto = respondentMapper.toDetailDto(aCase.getRespondent() != null ? aCase.getRespondent() : rootCase.getRespondent());
        IncidentDetailDto incidentDto = incidentDetailMapper.toDetailDto(aCase.getIncidentDetail() != null ? aCase.getIncidentDetail() : rootCase.getIncidentDetail());
        NarrativeDetailDto narrativeDto = narrativeStatementMapper.toDetailDto(aCase.getNarrativeStatement() != null ? aCase.getNarrativeStatement() : rootCase.getNarrativeStatement());

        List<CaseEvidence> evidenceList = caseEvidenceRepository.findByCasesId(aCase.getId());
        if ((evidenceList == null || evidenceList.isEmpty()) && !aCase.getId().equals(rootCase.getId())) {
            evidenceList = caseEvidenceRepository.findByCasesId(rootCase.getId());
        }

        EvidenceDetailDto evidenceDto = (evidenceList != null && !evidenceList.isEmpty()) ?
                caseEvidenceMapper.toDetailDto(evidenceList.get(0)) : null;
        List<EvidenceDetailDto> evidenceDtos = caseEvidenceMapper.toDetailDtoList(evidenceList);
        if (evidenceDtos == null) evidenceDtos = List.of();

        List<Witness> witnessSource = (aCase.getWitnesses() != null && !aCase.getWitnesses().isEmpty()) ?
                aCase.getWitnesses() : rootCase.getWitnesses();

        List<WitnessDetailDto> witnessDtos = (witnessSource != null && !witnessSource.isEmpty()) ?
                witnessSource.stream().map(witnessMapper::toDetailDto).toList() : List.of();

        List<CaseHistoryItemResponse> caseHistory = buildCaseHistory(aCase);

        String formattedDate = aCase.getCreatedAt() != null ?
                aCase.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) :
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        LocalDateTime rootCreatedAt = rootCase.getCreatedAt() != null ? rootCase.getCreatedAt() : aCase.getCreatedAt();
        String rootDateFiledFormatted = rootCreatedAt != null ?
                rootCreatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : formattedDate;

        String reopenRemarks = aCase.getReopenRemarks() != null ? aCase.getReopenRemarks() : rootCase.getReopenRemarks();

        Long receivingId = aCase.getCaseFilledBy() != null ? aCase.getCaseFilledBy().getId() : null;
        String receivingName = null;
        if (aCase.getCaseFilledBy() != null) {
            User u = aCase.getCaseFilledBy();
            if (u.getPerson() != null) {
                String fName = u.getPerson().getFirstName() != null ? u.getPerson().getFirstName() : "";
                String lName = u.getPerson().getLastName() != null ? u.getPerson().getLastName() : "";
                receivingName = (fName + " " + lName).trim();
            } else {
                receivingName = u.getUsername();
            }
        }

        if (receivingName == null || receivingName.isBlank()) {
            receivingName = receivingOfficerName;
        }

        Long assignedId = aCase.getOfficer() != null ? aCase.getOfficer().getId() : null;
        String assignedName = null;
        if (aCase.getOfficer() != null && aCase.getOfficer().getPerson() != null) {
            Person p = aCase.getOfficer().getPerson();
            assignedName = (p.getFirstName() != null ? p.getFirstName() : "") + " " + (p.getLastName() != null ? p.getLastName() : "");
            assignedName = assignedName.trim();
        } else {
            assignedName = assignedOfficerName;
        }
        return new BlotterCaseDetailResponse(
                aCase.getId(),
                aCase.getFilingType(),
                aCase.getCaseStatus(),
                aCase.getCaseStatusRemarks(),
                reopenRemarks,
                aCase.getCaseNumber(),
                rootCase.getCaseNumber(),
                rootCase.getId(),
                aCase.getPreviousCaseNumber(),
                aCase.getReOpenedCaseNumber(),
                formattedDate,
                rootCreatedAt,
                rootDateFiledFormatted,
                receivingId,
                receivingName,
                assignedId,
                assignedName,
                complainantDto,
                respondentDto,
                incidentDto,
                narrativeDto,
                evidenceDto,
                evidenceDtos,
                witnessDtos,
                caseHistory,
                aCase.getIsAttested(),
                aCase.getCreatedAt(),
                aCase.getUpdatedAt()
        );
    }

    /**
     * Updates case status (e.g., DISMISSED) and records status remarks and timeline event.
     *
     * @param id the blotter case ID
     * @param request status update request containing status and remarks
     * @return updated {@link BlotterCaseDetailResponse}
     */
    @Transactional
    public BlotterCaseDetailResponse updateCaseStatus(Long id, com.barangay.cases.blotter.dto.UpdateCaseStatusRequest request) {
        Case aCase = caseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blotter case not found with id: " + id));

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                CaseStatus newStatus = CaseStatus.valueOf(request.getStatus().toUpperCase().trim());
                aCase.setCaseStatus(newStatus);
                if (newStatus == CaseStatus.SETTLED) {
                    aCase.setSettledAt(LocalDateTime.now());
                    if (request.getRemarks() != null && !request.getRemarks().trim().isEmpty()) {
                        aCase.setSettlementTerms(request.getRemarks().trim());
                    }
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid CaseStatus value: {}", request.getStatus());
            }
        }

        if (request.getRemarks() != null && !request.getRemarks().trim().isEmpty()) {
            aCase.setCaseStatusRemarks(request.getRemarks().trim());
        }

        Case updated = caseRepository.save(aCase);

        try {
            String remarksText = request.getRemarks() != null ? request.getRemarks().trim() : "";
            String detailsText = remarksText.isBlank()
                    ? "Case status updated to " + updated.getCaseStatus().name()
                    : String.format("Case status updated to %s. Remarks: %s", updated.getCaseStatus().name(), remarksText);

            caseTimelineService.logEvent(
                    updated,
                    TimelineEventType.STATUS_CHANGED,
                    "Case Status Updated to " + updated.getCaseStatus().name(),
                    detailsText,
                    SecurityUtils.getCurrentUser()
            );
        } catch (Exception e) {
            log.warn("Failed to log timeline event for status update [caseId={}]: {}", id, e.getMessage(), e);
        }

        return toDetailResponse(updated, null, null);
    }

    /**
     * Reopens a closed, settled, or dismissed blotter case by creating a new case cycle.
     *
     * @param oldCaseId the ID of the closed or dismissed case
     * @param request   the reopen request payload containing remarks
     * @return {@link BlotterCaseDetailResponse} of the newly created reopened case
     */
    @Transactional
    public BlotterCaseDetailResponse reopenCase(Long oldCaseId, ReopenCaseRequest request) {
        // 1. Validation Guardrails
        Case oldCase = caseRepository.findById(oldCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Blotter case not found with id: " + oldCaseId));

        CaseStatus oldStatus = oldCase.getCaseStatus();
        boolean isClosedOrDismissed = oldStatus == CaseStatus.DISMISSED
                || oldStatus == CaseStatus.SETTLED
                || oldStatus == CaseStatus.WITHDRAWN
                || oldStatus == CaseStatus.EXPIRED_UNACTIONED;

        if (!isClosedOrDismissed) {
            throw new BadRequestException("Cannot reopen case with status: " + oldStatus + ". Only closed, settled, or dismissed cases can be reopened.");
        }

        if (oldCase.getReOpenedCaseNumber() != null && !oldCase.getReOpenedCaseNumber().trim().isEmpty()) {
            throw new BadRequestException("Case has already been reopened as " + oldCase.getReOpenedCaseNumber());
        }

        User currentUser = SecurityUtils.getCurrentUser();
        String remarksText = request.remarks() != null ? request.remarks().trim() : "";

        // 2. Create Brand New Case for Reopened Cycle
        Case newCase = new Case();
        String newCaseNumber = generateUniqueCaseNumber();

        newCase.setCaseNumber(newCaseNumber);
        newCase.setPreviousCaseNumber(oldCase.getCaseNumber());
        newCase.setFilingType(oldCase.getFilingType());
        newCase.setCaseStatus(CaseStatus.PENDING);
        newCase.setReopenRemarks(remarksText);
        newCase.setCaseStatusRemarks(remarksText);
        newCase.setSummonCount(0);
        newCase.setIsArchived(false);
        newCase.setIsAttested(false);
        newCase.setCaseFilledBy(currentUser);
        newCase.setDepartment(oldCase.getDepartment());
        newCase.setOfficer(oldCase.getOfficer());

        // Reset settlement fields
        newCase.setSettlementTerms(null);
        newCase.setSettledAt(null);
        newCase.setSettlementDocument(null);
        newCase.setSettlementVenue(null);

        // 3. One-to-One Entity Cloning (resolve sourceCase with fallback to root case if oldCase is missing details)
        Case sourceCase = oldCase;
        while ((sourceCase.getComplainant() == null || sourceCase.getIncidentDetail() == null) &&
                sourceCase.getPreviousCaseNumber() != null && !sourceCase.getPreviousCaseNumber().trim().isEmpty()) {
            java.util.Optional<Case> prev = caseRepository.findByCaseNumber(sourceCase.getPreviousCaseNumber().trim());
            if (prev.isPresent()) {
                sourceCase = prev.get();
            } else {
                break;
            }
        }

        if (sourceCase.getComplainant() != null) {
            Complainant oldComp = sourceCase.getComplainant();
            Complainant newComp = new Complainant();
            newComp.setPerson(oldComp.getPerson());
            newComp.setRelationshipToChild(oldComp.getRelationshipToChild());
            newCase.setComplainant(newComp);
        }

        if (sourceCase.getRespondent() != null) {
            Respondent oldResp = sourceCase.getRespondent();
            Respondent newResp = new Respondent();
            newResp.setPerson(oldResp.getPerson());
            newResp.setAlias(oldResp.getAlias());
            newResp.setRelationshipToComplainant(oldResp.getRelationshipToComplainant());
            newResp.setLivingWithComplainant(oldResp.getLivingWithComplainant());
            newCase.setRespondent(newResp);
        }

        if (sourceCase.getIncidentDetail() != null) {
            IncidentDetail oldInc = sourceCase.getIncidentDetail();
            IncidentDetail newInc = new IncidentDetail();
            newInc.setNatureOfComplaint(oldInc.getNatureOfComplaint());
            newInc.setFrequency(oldInc.getFrequency());
            newInc.setDateOfIncident(oldInc.getDateOfIncident());
            newInc.setTimeOfIncident(oldInc.getTimeOfIncident());
            newInc.setIncidentLocation(oldInc.getIncidentLocation());
            newInc.setInjuriesDamagesDescription(oldInc.getInjuriesDamagesDescription());
            newCase.setIncidentDetail(newInc);
        }

        if (sourceCase.getNarrativeStatement() != null) {
            NarrativeStatement oldNar = sourceCase.getNarrativeStatement();
            NarrativeStatement newNar = new NarrativeStatement();
            newNar.setFileName(oldNar.getFileName());
            newNar.setStatementSummary(oldNar.getStatementSummary());
            newNar.setFileData(oldNar.getFileData());
            newCase.setNarrativeStatement(newNar);
        }

        // Save new case
        Case savedNewCase = caseRepository.save(newCase);

        // Copy Witness records linking to same Person and new Case
        List<Witness> sourceWitnesses = (sourceCase.getWitnesses() != null && !sourceCase.getWitnesses().isEmpty()) ?
                sourceCase.getWitnesses() : oldCase.getWitnesses();

        if (sourceWitnesses != null && !sourceWitnesses.isEmpty()) {
            List<Witness> newWitnesses = new ArrayList<>();
            for (Witness oldW : sourceWitnesses) {
                Witness newW = new Witness();
                newW.setPerson(oldW.getPerson());
                newW.setACase(savedNewCase);
                newW.setTestimonyFile(oldW.getTestimonyFile());
                newW = witnessRepository.save(newW);
                newWitnesses.add(newW);
            }
            savedNewCase.setWitnesses(newWitnesses);
        }

        // Copy CaseEvidence records linking to new Case
        List<CaseEvidence> sourceEvidences = caseEvidenceRepository.findByCasesId(sourceCase.getId());
        if ((sourceEvidences == null || sourceEvidences.isEmpty()) && !sourceCase.getId().equals(oldCase.getId())) {
            sourceEvidences = caseEvidenceRepository.findByCasesId(oldCase.getId());
        }

        if (sourceEvidences != null && !sourceEvidences.isEmpty()) {
            for (CaseEvidence oldEv : sourceEvidences) {
                CaseEvidence newEv = new CaseEvidence();
                newEv.setCases(savedNewCase);
                newEv.setType(oldEv.getType());
                newEv.setFiles(oldEv.getFiles());
                newEv.setFileName(oldEv.getFileName());
                newEv.setDescription(oldEv.getDescription());
                caseEvidenceRepository.save(newEv);
            }
        }

        // 4. Update Old Case with reOpenedCaseNumber and reopenRemarks (retaining its original DISMISSED/SETTLED status)
        oldCase.setReopenRemarks(remarksText);
        oldCase.setReOpenedCaseNumber(newCaseNumber);
        oldCase.setCaseUpdatedBy(currentUser);
        caseRepository.save(oldCase);

        // 5. Timeline Events
        try {
            // Log event on Old Case
            caseTimelineService.logEvent(
                    oldCase,
                    TimelineEventType.STATUS_CHANGED,
                    "Case Reopened",
                    String.format("Case reopened as %s. Remarks: %s", newCaseNumber, remarksText),
                    currentUser
            );

            // Log event on New Case
            caseTimelineService.logEvent(
                    savedNewCase,
                    TimelineEventType.CASE_FILED,
                    "Case Created (Reopened)",
                    String.format("Reopened from previous case %s. Remarks: %s", oldCase.getCaseNumber(), remarksText),
                    currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to log timeline event for case reopening [oldCaseId={}, newCaseNumber={}]: {}", oldCaseId, newCaseNumber, e.getMessage());
        }

        return toDetailResponse(savedNewCase, null, null);
    }
}
