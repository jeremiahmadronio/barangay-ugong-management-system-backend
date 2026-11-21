package com.barangay.cases.blotter.service;

import com.barangay.cases.blotter.dto.ExtendCaseRequest;
import com.barangay.cases.blotter.dto.UpdateCaseStatusRequest;
import com.barangay.cases.shared.model.CertificateToFileAction;
import com.barangay.cases.shared.repository.CertificateToFileActionRepository;
import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.zshared.dto.RespondentCreateDto;
import com.barangay.zshared.enumerated.DocumentReissuanceStatus;
import com.barangay.zshared.enumerated.RelatedBarangayService;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.barangay.cases.blotter.dto.*;
import com.barangay.cases.shared.model.*;
import com.barangay.cases.shared.repository.CaseSessionRepository;
import com.barangay.zshared.enumerated.SessionStatus;
import com.barangay.zshared.exception.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CaseSessionRepository caseSessionRepository;
    private final CertificateToFileActionRepository certificateToFileActionRepository;
    private final FirstTimeJobSeekerRepository firstTimeJobSeekerRepository;

    private final ComplainantMapper complainantMapper;
    private final RespondentMapper respondentMapper;
    private final IncidentDetailMapper incidentDetailMapper;
    private final NarrativeStatementMapper narrativeStatementMapper;
    private final CaseEvidenceMapper caseEvidenceMapper;
    private final WitnessMapper witnessMapper;

    /**
     * Retrieves paginated and filtered Blotter cases without using findAll().
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
     */
    private BlotterTableRowResponse toTableRowResponse(Case rootCase) {
        if (rootCase == null) return null;

        Case trueRoot = rootCase;
        while (trueRoot.getPreviousCaseNumber() != null && !trueRoot.getPreviousCaseNumber().trim().isEmpty()) {
            Optional<Case> prevOpt = caseRepository.findByCaseNumber(trueRoot.getPreviousCaseNumber().trim());
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
            Optional<Case> nextOpt = caseRepository.findByCaseNumber(curr.getReOpenedCaseNumber().trim());
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
     * Retrieves single blotter case details by ID including full complainant, respondent,.
     */
    @Transactional(readOnly = true)
    public BlotterCaseDetailResponse getBlotterCaseById(Long id) {
        Case aCase = caseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blotter case not found with id: " + id));

        return toDetailResponse(aCase, null, null);
    }

    /**
     * Calculates Blotter KPI Statistics with month-to-date vs full previous calendar month trend percentages.
     */
    @Transactional(readOnly = true)
    public BlotterStatsResponse getBlotterStats() {
        LocalDateTime currentMonthStart = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime prevMonthStart = YearMonth.now().minusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime prevMonthEnd = currentMonthStart.minusNanos(1);

        // 1. Total Blotter Cases
        long totalCases = caseRepository.countRootBlotterCases();
        long currentTotal = caseRepository.countRootBlotterCasesByCreatedAtGreaterThanEqual(currentMonthStart);
        long prevTotal = caseRepository.countRootBlotterCasesByCreatedAtBetween(prevMonthStart, prevMonthEnd);
        String totalCasesTrend = calculateMonthlyTrend(currentTotal, prevTotal);

        // 2. Total For the Record Only
        long totalForTheRecord = caseRepository.countRootBlotterCasesByFilingType(FilingType.FOR_RECORD_ONLY);
        long currentForRecord = caseRepository.countRootBlotterCasesByFilingTypeAndCreatedAtGreaterThanEqual(FilingType.FOR_RECORD_ONLY, currentMonthStart);
        long prevForRecord = caseRepository.countRootBlotterCasesByFilingTypeAndCreatedAtBetween(FilingType.FOR_RECORD_ONLY, prevMonthStart, prevMonthEnd);
        String totalForTheRecordTrend = calculateMonthlyTrend(currentForRecord, prevForRecord);

        // 3. Under Mediation / In Progress
        List<CaseStatus> activeStatuses = List.of(CaseStatus.UNDER_MEDIATION);
        long inProgressCases = caseRepository.countLatestBlotterCasesByCaseStatusIn(activeStatuses);

        // 4. Settled / Resolved Cases (Gamit ang settledAt para sa accurate monthly resolution metrics)
        List<CaseStatus> resolvedStatuses = List.of(CaseStatus.SETTLED);
        long resolvedCases = caseRepository.countLatestSettledBlotterCases(resolvedStatuses);
        long currentResolved = caseRepository.countLatestSettledBlotterCasesBySettledAtGreaterThanEqual(resolvedStatuses, currentMonthStart);
        long prevResolved = caseRepository.countLatestSettledBlotterCasesBySettledAtBetween(resolvedStatuses, prevMonthStart, prevMonthEnd);
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

        // 3. Respondent (Optional for For the Record or records without an identified respondent)
        if (request.respondent() != null && hasValidRespondent(request.respondent())) {
            Person respPerson = respondentMapper.toNewPerson(request.respondent());
            if (respPerson != null) {
                if (respPerson.getLastName() == null || respPerson.getLastName().isBlank()) {
                    respPerson.setLastName("N/A");
                }
                if (respPerson.getFirstName() == null || respPerson.getFirstName().isBlank()) {
                    respPerson.setFirstName("Unknown");
                }
                respPerson = personRepository.save(respPerson);
                Respondent respondent = respondentMapper.toRespondent(respPerson, request.respondent());
                aCase.setRespondent(respondent);
            }
        } else {
            aCase.setRespondent(null);
        }

        // 4. Incident Detail
        if (request.incident() != null) {
            IncidentDetail incidentDetail = incidentDetailMapper.fromCreateDto(request.incident());
            aCase.setIncidentDetail(incidentDetail);
        }

        // 4.1. Related Barangay Service & FTJS Re-issuance Linkage
        String rawRelService = request.relatedBarangayService();
        if ((rawRelService == null || rawRelService.isBlank()) && request.incident() != null) {
            rawRelService = request.incident().relatedBarangayService();
        }
        if (rawRelService != null && !rawRelService.isBlank()) {
            try {
                RelatedBarangayService relService =
                        RelatedBarangayService.valueOf(rawRelService.trim().toUpperCase());
                aCase.setRelatedBarangayService(relService);
                if (relService == RelatedBarangayService.FTJS_REISSUANCE) {
                    aCase.setDocumentReissuanceStatus(DocumentReissuanceStatus.APPROVED_FOR_REISSUANCE);
                    aCase.setDocumentReissuanceReviewedAt(LocalDateTime.now());
                    if (currentUser != null) {
                        aCase.setDocumentReissuanceReviewedBy(currentUser);
                    }
                    if (request.ftjsApplicationId() != null) {
                        firstTimeJobSeekerRepository.findById(request.ftjsApplicationId()).ifPresent(aCase::setFtjsApplication);
                    }
                }
            } catch (Exception ignored) {}
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
     * Updates an existing Blotter Case with modified fields.
     */
    @Transactional
    public BlotterCaseDetailResponse updateBlotterCase(Long caseId, BlotterCaseUpdateRequest request) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + caseId));

        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            aCase.setCaseUpdatedBy(currentUser);
        }

        // 1. Update Respondent relationship & living status
        if (aCase.getRespondent() != null) {
            if (request.relationshipToComplainant() != null) {
                aCase.getRespondent().setRelationshipToComplainant(request.relationshipToComplainant());
            }
            if (request.currentlyLivingWithComplainant() != null) {
                aCase.getRespondent().setLivingWithComplainant(request.currentlyLivingWithComplainant());
            }
        }

        // 2. Update Incident Details
        if (request.incident() != null) {
            IncidentDetail inc = incidentDetailMapper.fromCreateDto(request.incident());
            aCase.setIncidentDetail(inc);
        }

        // 3. Update Narrative Statement
        if (request.narrative() != null) {
            if (aCase.getNarrativeStatement() == null) {
                aCase.setNarrativeStatement(narrativeStatementMapper.fromCreateDto(request.narrative()));
            } else {
                NarrativeStatement narr = aCase.getNarrativeStatement();
                if (request.narrative().statementSummary() != null) {
                    narr.setStatementSummary(request.narrative().statementSummary());
                }
                if (request.narrative().fileName() != null && !request.narrative().fileName().isBlank()) {
                    narr.setFileName(request.narrative().fileName());
                }
                if (request.narrative().fileData() != null && request.narrative().fileData().length > 0) {
                    narr.setFileData(request.narrative().fileData());
                }
            }
        }

        // 4. Update Evidences (Selective update without losing existing evidence files)
        if (request.evidences() != null) {
            List<CaseEvidence> existingEvidences = caseEvidenceRepository.findByCasesId(aCase.getId());
            Map<String, CaseEvidence> existingEvMapByName = new HashMap<>();
            Map<Long, CaseEvidence> existingEvMapById = new HashMap<>();
            for (CaseEvidence ce : existingEvidences) {
                if (ce.getId() != null) {
                    existingEvMapById.put(ce.getId(), ce);
                }
                if (ce.getType() != null && ce.getType().getName() != null) {
                    existingEvMapByName.put(ce.getType().getName().trim().toLowerCase(), ce);
                }
            }

            List<CaseEvidence> updatedEvidences = new ArrayList<>();
            for (EvidenceCreateDto eDto : request.evidences()) {
                String typeName = (eDto.evidenceTypeName() != null) ? eDto.evidenceTypeName().trim() : "General Evidence";
                CaseEvidence targetEvidence = null;
                if (eDto.id() != null && existingEvMapById.containsKey(eDto.id())) {
                    targetEvidence = existingEvMapById.get(eDto.id());
                } else if (existingEvMapByName.containsKey(typeName.toLowerCase())) {
                    targetEvidence = existingEvMapByName.get(typeName.toLowerCase());
                }

                if (targetEvidence != null) {
                    // If new file provided, update file bytes and name; otherwise PRESERVE existing file
                    if (eDto.files() != null && eDto.files().length > 0) {
                        targetEvidence.setFiles(eDto.files());
                        if (eDto.fileName() != null && !eDto.fileName().isBlank()) {
                            targetEvidence.setFileName(eDto.fileName().trim());
                        }
                    }
                    if (eDto.description() != null) {
                        targetEvidence.setDescription(eDto.description());
                    }
                    targetEvidence = caseEvidenceRepository.save(targetEvidence);
                    updatedEvidences.add(targetEvidence);
                } else {
                    // New evidence added during edit
                    CaseEvidence newEv = caseEvidenceMapper.fromCreateDto(eDto, aCase);
                    if (newEv != null) {
                        newEv = caseEvidenceRepository.save(newEv);
                        updatedEvidences.add(newEv);
                    }
                }
            }

            // Remove any evidence that was unselected by the user
            for (CaseEvidence ce : existingEvidences) {
                if (!updatedEvidences.contains(ce)) {
                    caseEvidenceRepository.delete(ce);
                }
            }
        }

        // 5. Update Witnesses (Selective in-place update without losing testimony files)
        if (request.witnesses() != null) {
            List<Witness> currentWitnessList = (aCase.getWitnesses() != null && !aCase.getWitnesses().isEmpty())
                    ? new ArrayList<>(aCase.getWitnesses())
                    : witnessRepository.findByCaseId(aCase.getId());

            Map<Long, Witness> existingWitnessById = new HashMap<>();
            Map<Integer, Witness> existingWitnessByIndex = new HashMap<>();
            int idx = 0;
            for (Witness w : currentWitnessList) {
                if (w.getId() != null) {
                    existingWitnessById.put(w.getId(), w);
                }
                existingWitnessByIndex.put(idx++, w);
            }

            List<Witness> updatedWitnessEntities = new ArrayList<>();
            int dtoIndex = 0;
            for (WitnessCreateDto wDto : request.witnesses()) {
                Witness targetWitness = null;
                if (wDto.id() != null && existingWitnessById.containsKey(wDto.id())) {
                    targetWitness = existingWitnessById.get(wDto.id());
                } else if (existingWitnessByIndex.containsKey(dtoIndex)) {
                    targetWitness = existingWitnessByIndex.get(dtoIndex);
                }

                if (targetWitness != null) {
                    // Update person info
                    Person p = targetWitness.getPerson();
                    if (p == null) {
                        p = new Person();
                    }
                    if (wDto.firstName() != null) p.setFirstName(wDto.firstName().trim());
                    if (wDto.lastName() != null) p.setLastName(wDto.lastName().trim());
                    if (wDto.contactNumber() != null) p.setContactNumber(wDto.contactNumber().trim());
                    p = personRepository.save(p);
                    targetWitness.setPerson(p);

                    // Update testimony file ONLY IF new file is provided; otherwise PRESERVE existing
                    if (wDto.testimonyFile() != null && wDto.testimonyFile().length > 0) {
                        targetWitness.setTestimonyFile(wDto.testimonyFile());
                        if (wDto.testimonyFileName() != null && !wDto.testimonyFileName().isBlank()) {
                            targetWitness.setTestimonyFileName(wDto.testimonyFileName().trim());
                        }
                    }
                    targetWitness = witnessRepository.save(targetWitness);
                    updatedWitnessEntities.add(targetWitness);
                } else {
                    // Brand new witness added in Edit Modal
                    Person newPerson = witnessMapper.toNewPerson(wDto);
                    if (newPerson == null) {
                        newPerson = new Person();
                        newPerson.setFirstName(wDto.firstName() != null ? wDto.firstName().trim() : "Witness");
                        newPerson.setLastName(wDto.lastName() != null ? wDto.lastName().trim() : "Statement");
                        if (wDto.contactNumber() != null) newPerson.setContactNumber(wDto.contactNumber().trim());
                    }
                    newPerson = personRepository.save(newPerson);
                    Witness newW = witnessMapper.toWitness(newPerson, aCase, wDto);
                    newW = witnessRepository.save(newW);
                    updatedWitnessEntities.add(newW);
                }
                dtoIndex++;
            }

            if (aCase.getWitnesses() != null) {
                aCase.getWitnesses().clear();
                aCase.getWitnesses().addAll(updatedWitnessEntities);
            } else {
                aCase.setWitnesses(new ArrayList<>(updatedWitnessEntities));
            }
        }

        Case updatedCase = caseRepository.save(aCase);

        try {
            caseTimelineService.logEvent(
                    updatedCase,
                    TimelineEventType.STATUS_CHANGED,
                    "Case Details Updated",
                    "Case details, evidences, or testimonies updated via Edit Case.",
                    currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to log timeline event for case update [caseId={}]: {}", updatedCase.getId(), e.getMessage());
        }

        return getBlotterCaseById(updatedCase.getId());
    }

    /**
     * Retrieves all timeline events for a case.
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
     * Traverses the case lineage (both backwards via previousCaseNumber and forwards via reOpenedCaseNumber).
     */
    private List<CaseHistoryItemResponse> buildCaseHistory(Case currentCase) {
        if (currentCase == null) return List.of();

        // 1. Find root case
        Case rootCase = currentCase;
        while (rootCase.getPreviousCaseNumber() != null && !rootCase.getPreviousCaseNumber().trim().isEmpty()) {
            Optional<Case> prevOpt = caseRepository.findByCaseNumber(rootCase.getPreviousCaseNumber().trim());
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
            Optional<Case> nextOpt = caseRepository.findByCaseNumber(curr.getReOpenedCaseNumber().trim());
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
            Optional<Case> prevOpt = caseRepository.findByCaseNumber(rootCase.getPreviousCaseNumber().trim());
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

        String extendedByName = null;
        if (aCase.getExtendedBy() != null && aCase.getExtendedBy().getPerson() != null) {
            Person ep = aCase.getExtendedBy().getPerson();
            extendedByName = ((ep.getFirstName() != null ? ep.getFirstName() : "") + " " + (ep.getLastName() != null ? ep.getLastName() : "")).trim();
        } else if (aCase.getExtendedBy() != null) {
            extendedByName = aCase.getExtendedBy().getUsername();
        }

        String ftjsCertNo = aCase.getFtjsApplication() != null ? aCase.getFtjsApplication().getFtjsNumber() : null;
        Long ftjsAppId = aCase.getFtjsApplication() != null ? aCase.getFtjsApplication().getId() : null;
        String relService = aCase.getRelatedBarangayService() != null ? aCase.getRelatedBarangayService().name() : null;
        String docReissStatus = aCase.getDocumentReissuanceStatus() != null ? aCase.getDocumentReissuanceStatus().name() : null;
        String reissReviewedBy = null;
        if (aCase.getDocumentReissuanceReviewedBy() != null) {
            User ru = aCase.getDocumentReissuanceReviewedBy();
            if (ru.getPerson() != null) {
                reissReviewedBy = ((ru.getPerson().getFirstName() != null ? ru.getPerson().getFirstName() : "") + " " +
                        (ru.getPerson().getLastName() != null ? ru.getPerson().getLastName() : "")).trim();
            } else {
                reissReviewedBy = ru.getUsername();
            }
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
                Boolean.TRUE.equals(aCase.getIsExtended()),
                aCase.getExtensionCount() != null ? aCase.getExtensionCount() : 0,
                aCase.getExtendedAt(),
                aCase.getExtensionReason(),
                extendedByName,
                aCase.getCreatedAt(),
                aCase.getUpdatedAt(),
                relService,
                ftjsAppId,
                ftjsCertNo,
                docReissStatus,
                aCase.getDocumentReissuanceRemarks(),
                reissReviewedBy,
                aCase.getDocumentReissuanceReviewedAt()
        );
    }

    @Transactional
    public BlotterCaseDetailResponse updateCaseStatus(Long id, UpdateCaseStatusRequest request) {
        Case aCase = caseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blotter case not found with id: " + id));

        if (Boolean.TRUE.equals(aCase.getIsDeleted()) || Boolean.TRUE.equals(aCase.getIsPermanentDeleted())) {
            throw new IllegalStateException("Cannot update status of a deleted case.");
        }

        CaseStatus newStatus = null;
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            try {
                newStatus = CaseStatus.valueOf(request.getStatus().toUpperCase().trim());
                aCase.setCaseStatus(newStatus);

                if (newStatus == CaseStatus.SETTLED) {
                    aCase.setSettledAt(LocalDateTime.now());
                    if (request.getRemarks() != null && !request.getRemarks().trim().isEmpty()) {
                        aCase.setSettlementTerms(request.getRemarks().trim());
                    }
                }
            } catch (IllegalArgumentException e) {
                log.error("Invalid CaseStatus value provided: {}", request.getStatus());
                throw new IllegalArgumentException("Invalid case status: " + request.getStatus());
            }
        }

        if (request.getRemarks() != null && !request.getRemarks().trim().isEmpty()) {
            aCase.setCaseStatusRemarks(request.getRemarks().trim());
        }

        User currentUser = SecurityUtils.getCurrentUser();
        aCase.setCaseUpdatedBy(currentUser);

        if (newStatus == CaseStatus.SETTLED || newStatus == CaseStatus.DISMISSED || newStatus == CaseStatus.WITHDRAWN || newStatus == CaseStatus.EXPIRED_UNACTIONED || newStatus == CaseStatus.CERTIFIED_TO_FILE_ACTION) {
            cancelActiveMediationSessions(aCase.getId(), newStatus);
        }

        if (newStatus == CaseStatus.CERTIFIED_TO_FILE_ACTION) {
            aCase.setIsAttested(true);
            try {
                CertificateToFileAction cfa = certificateToFileActionRepository
                        .findByCases_Id(aCase.getId())
                        .orElse(new CertificateToFileAction());
                cfa.setCases(aCase);
                cfa.setGrounds(request.getRemarks() != null && !request.getRemarks().trim().isEmpty()
                        ? request.getRemarks().trim()
                        : "Walang naganap na pagkakasundo sa pagitan ng mga partido sa kabila ng pagsisikap na maayos ang alitan.");
                String subject = (aCase.getIncidentDetail() != null && aCase.getIncidentDetail().getNatureOfComplaint() != null)
                        ? aCase.getIncidentDetail().getNatureOfComplaint()
                        : "Barangay Dispute Settlement";
                cfa.setSubjectOfLitigation(subject);
                if (cfa.getControlNumber() == null || cfa.getControlNumber().isBlank()) {
                    cfa.setControlNumber("CFA-" + aCase.getCaseNumber());
                }
                if (cfa.getIssuedBy() == null) {
                    cfa.setIssuedBy(currentUser);
                }
                cfa.setUpdatedBy(currentUser);
                certificateToFileActionRepository.save(cfa);
            } catch (Exception ex) {
                log.warn("Failed to persist CertificateToFileAction entity [caseId={}]: {}", aCase.getId(), ex.getMessage());
            }
        }

        // 2. Persist Case Entity
        Case updated = caseRepository.save(aCase);

        // 3. Log Timeline Event
        try {
            String remarksText = request.getRemarks() != null ? request.getRemarks().trim() : "";
            TimelineEventType eventType = TimelineEventType.STATUS_CHANGED;
            String eventTitle;
            String detailsText;

            if (newStatus == CaseStatus.CERTIFIED_TO_FILE_ACTION) {
                eventType = TimelineEventType.DOCUMENT_GENERATED;
                eventTitle = "Certificate to File Action (CFA) Issued";
                detailsText = remarksText.isBlank()
                        ? "Issued Certificate to File Action (KP Form 20) under Katarungang Pambarangay."
                        : "Issued Certificate to File Action (KP Form 20). Grounds: " + remarksText;
            } else if (newStatus == CaseStatus.SETTLED) {
                eventTitle = "Case Settled";
                detailsText = remarksText.isBlank()
                        ? "Case marked as settled with agreement."
                        : "Case settled. Agreement terms: " + remarksText;
            } else if (newStatus == CaseStatus.DISMISSED) {
                eventTitle = "Case Dismissed";
                detailsText = remarksText.isBlank()
                        ? "Case dismissed."
                        : "Case dismissed. Remarks: " + remarksText;
            } else if (newStatus == CaseStatus.WITHDRAWN) {
                eventTitle = "Complaint Withdrawn";
                detailsText = remarksText.isBlank()
                        ? "Complaint withdrawn by complainant."
                        : "Complaint withdrawn. Remarks: " + remarksText;
            } else if (newStatus == CaseStatus.REOPENED) {
                eventTitle = "Case Reopened";
                detailsText = remarksText.isBlank()
                        ? "Case reopened for new proceedings."
                        : "Case reopened. Reason: " + remarksText;
            } else {
                String statusName = updated.getCaseStatus() != null ? updated.getCaseStatus().name().replace('_', ' ') : "Updated";
                eventTitle = "Case Status Updated to " + statusName;
                detailsText = remarksText.isBlank()
                        ? "Case status updated to " + statusName + "."
                        : String.format("Case status updated to %s. Remarks: %s", statusName, remarksText);
            }

            caseTimelineService.logEvent(
                    updated,
                    eventType,
                    eventTitle,
                    detailsText,
                    currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to log timeline event for status update [caseId={}]: {}", id, e.getMessage(), e);
        }

        return toDetailResponse(updated, null, null);
    }

    /**
     * Helper method para kanselahin ang lahat ng SCHEDULED sessions ng case.
     */
    private void cancelActiveMediationSessions(Long caseId, CaseStatus newStatus) {
        List<CaseSession> scheduledSessions = caseSessionRepository.findSessionsByCaseIdAndStatus(
                caseId,
                SessionStatus.SCHEDULED
        );

        if (!scheduledSessions.isEmpty()) {
            for (CaseSession session : scheduledSessions) {
                session.setStatus(SessionStatus.CANCELLED);
                session.setNotes("System: Automatically cancelled because case was marked as " + newStatus.name() + ".");
            }
            caseSessionRepository.saveAll(scheduledSessions);
        }
    }

    /**
     * Reopens a closed, settled, or dismissed blotter case by creating a new case cycle.
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
            Optional<Case> prev = caseRepository.findByCaseNumber(sourceCase.getPreviousCaseNumber().trim());
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

    /**
     * Extends the case mediation period by 15 days (one-time allowable extension).
     */
    @Transactional
    public BlotterCaseDetailResponse extendCase(Long id, ExtendCaseRequest request) {
        Case aCase = caseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + id));

        if (Boolean.TRUE.equals(aCase.getIsDeleted()) || Boolean.TRUE.equals(aCase.getIsPermanentDeleted())) {
            throw new IllegalStateException("Cannot extend a deleted case.");
        }
        if (Boolean.TRUE.equals(aCase.getIsArchived())) {
            throw new IllegalStateException("Cannot extend an archived case.");
        }
        if (aCase.getCaseStatus() == CaseStatus.SETTLED || aCase.getCaseStatus() == CaseStatus.DISMISSED || aCase.getCaseStatus() == CaseStatus.ISSUED_REFERRAL) {
            throw new IllegalStateException("Cannot extend a case that is already " + aCase.getCaseStatus() + ".");
        }
        if (Boolean.TRUE.equals(aCase.getIsExtended()) || (aCase.getExtensionCount() != null && aCase.getExtensionCount() >= 1)) {
            throw new IllegalStateException("This case has already been granted a 15-day extension. Cases can only be extended once.");
        }

        LocalDateTime now = LocalDateTime.now();
        User currentUser = SecurityUtils.getCurrentUser();

        aCase.setIsExtended(true);
        aCase.setExtensionCount(1);
        aCase.setExtendedAt(now);
        aCase.setExtensionReason(request.reason());
        aCase.setExtendedBy(currentUser);
        aCase.setCaseUpdatedBy(currentUser);

        caseRepository.save(aCase);

        try {
            caseTimelineService.logEvent(
                    aCase,
                    TimelineEventType.CONCILIATION_EXTENDED,
                    "Mediation Period Extended (15 Days)",
                    "15-day extension granted under Katarungang Pambarangay. Reason: " + request.reason(),
                    currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to log timeline event for case extension [caseId={}]: {}", id, e.getMessage());
        }

        return getBlotterCaseById(aCase.getId());
    }

    /**
     * Checks if respondent information contains real, identifying details.
     */
    private boolean hasValidRespondent(RespondentCreateDto dto) {
        if (dto == null) return false;
        String first = dto.firstName() != null ? dto.firstName().trim() : "";
        String last = dto.lastName() != null ? dto.lastName().trim() : "";
        if (first.isEmpty() && last.isEmpty()) return false;
        if ((first.equalsIgnoreCase("Respondent") || first.equalsIgnoreCase("N/A") || first.equalsIgnoreCase("None") || first.isEmpty()) &&
            (last.equalsIgnoreCase("N/A") || last.equalsIgnoreCase("Respondent") || last.equalsIgnoreCase("None") || last.isEmpty())) {
            return false;
        }
        return true;
    }

    /**
     * Searches active, eligible FTJS certificate applications for linking to an Affidavit of Loss case.
     */
    @Transactional(readOnly = true)
    public List<FtjsSearchDTO> searchFtjsApplications(String query) {
        Pageable pageable = PageRequest.of(0, 20);
        List<FirstTimeJobSeeker> results =
                firstTimeJobSeekerRepository.searchEligibleForReissuance(query != null ? query.trim() : "", pageable);

        return results.stream().map(ftjs -> {
            Person p = ftjs.getPerson();
            String applicantName = p != null ? (p.getFirstName() + " " + p.getLastName()).trim() : "N/A";

            List<Case> activeCases = caseRepository.findActiveDocumentReissuanceRequests(
                    ftjs.getId(),
                    RelatedBarangayService.FTJS_REISSUANCE,
                    List.of(DocumentReissuanceStatus.PENDING_VERIFICATION, DocumentReissuanceStatus.APPROVED_FOR_REISSUANCE)
            );
            boolean hasActive = !activeCases.isEmpty();
            String activeBlotterNumber = hasActive ? activeCases.get(0).getCaseNumber() : null;
            String activeBlotterStatus = hasActive && activeCases.get(0).getDocumentReissuanceStatus() != null
                    ? activeCases.get(0).getDocumentReissuanceStatus().name()
                    : null;

            return new FtjsSearchDTO(
                    ftjs.getId(),
                    ftjs.getFtjsNumber(),
                    applicantName,
                    p != null ? p.getFirstName() : null,
                    p != null ? p.getMiddleName() : null,
                    p != null ? p.getLastName() : null,
                    p != null ? p.getSuffix() : null,
                    p != null ? p.getContactNumber() : null,
                    p != null ? p.getCompleteAddress() : null,
                    p != null ? p.getEmail() : null,
                    p != null ? p.getCivilStatus() : null,
                    (p != null && p.getAge() != null) ? p.getAge().intValue() : null,
                    p != null ? p.getGender() : null,
                    ftjs.getResident() != null ? ftjs.getResident().getResidentId() : null,
                    p != null ? p.getId() : null,
                    ftjs.getDateSubmitted(),
                    ftjs.getIssuanceCount(),
                    hasActive,
                    activeBlotterNumber,
                    activeBlotterStatus
            );
        }).toList();
    }

    /**
     * Verifies or rejects a document re-issuance clearance for an Affidavit of Loss Blotter case.
     */
    @Transactional
    public void verifyDocumentReissuance(Long caseId, VerifyDocumentReissuanceDTO dto) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Blotter case not found: " + caseId));

        if (aCase.getRelatedBarangayService() != RelatedBarangayService.FTJS_REISSUANCE) {
            throw new IllegalArgumentException("This Blotter case is not eligible for FTJS reissuance verification.");
        }

        if (aCase.getDocumentReissuanceStatus() != DocumentReissuanceStatus.PENDING_VERIFICATION) {
            throw new IllegalStateException("Cannot change verification status from current state: " + aCase.getDocumentReissuanceStatus());
        }

        User currentUser = SecurityUtils.getCurrentUser();
        String action = dto.action() != null ? dto.action().trim().toUpperCase(Locale.ROOT) : "";

        if ("REJECT".equals(action)) {
            if (dto.remarks() == null || dto.remarks().trim().isEmpty()) {
                throw new IllegalArgumentException("Remarks are required when rejecting document reissuance.");
            }
            aCase.setDocumentReissuanceStatus(DocumentReissuanceStatus.REJECTED);
            aCase.setDocumentReissuanceRemarks(dto.remarks().trim());
            aCase.setDocumentReissuanceReviewedBy(currentUser);
            aCase.setDocumentReissuanceReviewedAt(LocalDateTime.now());
            caseRepository.save(aCase);

            try {
                caseTimelineService.logEvent(
                        aCase,
                        TimelineEventType.STATUS_CHANGED,
                        "Reissuance Rejected",
                        "Reissuance clearance was rejected by officer. Reason: " + dto.remarks().trim(),
                        currentUser
                );
            } catch (Exception e) {
                log.warn("Failed to log timeline event: {}", e.getMessage());
            }
        } else if ("APPROVE".equals(action)) {
            aCase.setDocumentReissuanceStatus(DocumentReissuanceStatus.APPROVED_FOR_REISSUANCE);
            aCase.setDocumentReissuanceRemarks(dto.remarks() != null && !dto.remarks().trim().isEmpty() ? dto.remarks().trim() : "Approved for document reissuance.");
            aCase.setDocumentReissuanceReviewedBy(currentUser);
            aCase.setDocumentReissuanceReviewedAt(LocalDateTime.now());
            caseRepository.save(aCase);

            try {
                caseTimelineService.logEvent(
                        aCase,
                        TimelineEventType.STATUS_CHANGED,
                        "Cleared for Document Reissuance",
                        "Case was verified and cleared for certificate reissuance.",
                        currentUser
                );
            } catch (Exception e) {
                log.warn("Failed to log timeline event: {}", e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("Invalid action: '" + dto.action() + "'. Allowed values: APPROVE, REJECT.");
        }
    }
}
