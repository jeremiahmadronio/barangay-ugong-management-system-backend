package com.barangay.cases.vawc.service;

import com.barangay.cases.blotter.dto.UpdateCaseStatusRequest;
import com.barangay.cases.shared.model.*;
import com.barangay.cases.shared.repository.CaseEvidenceRepository;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.cases.vawc.dto.*;
import com.barangay.cases.vawc.model.BarangayProtectionOrder;
import com.barangay.cases.vawc.model.ChildInvolved;
import com.barangay.cases.vawc.model.ReferralFeedback;
import com.barangay.cases.vawc.model.VawcReferral;
import com.barangay.cases.vawc.model.ViolenceType;
import com.barangay.cases.vawc.repository.BarangayProtectionOrderRepository;
import com.barangay.cases.vawc.repository.ChildInvolvedRepository;
import com.barangay.cases.vawc.repository.ReferralFeedbackRepository;
import com.barangay.cases.vawc.repository.VawcReferralRepository;
import com.barangay.cases.vawc.repository.ViolenceTypeRepository;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.people.witness.model.Witness;
import com.barangay.people.witness.repository.WitnessRepository;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.dto.*;
import com.barangay.zshared.enumerated.BpoStatus;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import com.barangay.zshared.enumerated.TimelineEventType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.mapper.*;
import com.barangay.zshared.mapper.PageResponse;
import com.barangay.zshared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.barangay.cases.blotter.dto.TimelineEventResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class VawcCaseService {

    private final CaseRepository caseRepository;
    private final DepartmentRepository departmentRepository;
    private final OfficerRepository officerRepository;
    private final PersonRepository personRepository;
    private final WitnessRepository witnessRepository;
    private final CaseEvidenceRepository caseEvidenceRepository;
    private final ChildInvolvedRepository childInvolvedRepository;
    private final ViolenceTypeRepository violenceTypeRepository;
    private final BarangayProtectionOrderRepository barangayProtectionOrderRepository;
    private final VawcReferralRepository vawcReferralRepository;
    private final ReferralFeedbackRepository referralFeedbackRepository;

    private final ComplainantMapper complainantMapper;
    private final RespondentMapper respondentMapper;
    private final IncidentDetailMapper incidentDetailMapper;
    private final NarrativeStatementMapper narrativeStatementMapper;
    private final WitnessMapper witnessMapper;
    private final CaseEvidenceMapper caseEvidenceMapper;
    private final CaseTimelineService caseTimelineService;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Creates a new VAWC Case intake record.
     */
    @Transactional
    public VawcCaseDetailResponse createVawcCase(VawcCaseCreateRequest request) {
        Case aCase = new Case();

        // Filing Type & Case Number
        FilingType filingType = request.filingType() != null ? request.filingType() : FilingType.FORMAL_COMPLAINT;
        if (request.filingMethod() != null && request.filingMethod().equalsIgnoreCase("ORAL_REPORT_ONLY")) {
            filingType = FilingType.FOR_RECORD_ONLY;
        }
        aCase.setFilingType(filingType);

        String caseNumber = request.caseNumber();
        if (caseNumber == null || caseNumber.isBlank() || caseRepository.existsByCaseNumber(caseNumber.trim())) {
            caseNumber = generateUniqueVawcCaseNumber();
        }
        aCase.setCaseNumber(caseNumber);
        aCase.setCaseStatus(filingType == FilingType.FOR_RECORD_ONLY ? CaseStatus.RECORDED : CaseStatus.PENDING);
        aCase.setIsArchived(false);
        aCase.setAttestedAt(LocalDateTime.now());

        // Department & Logged-in User
        departmentRepository.findByName("VAWC").ifPresent(aCase::setDepartment);
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            aCase.setCaseFilledBy(currentUser);
        }

        // Complainant
        if (request.complainant() != null) {
            Person compPerson = complainantMapper.toNewPerson(request.complainant());
            if (compPerson != null) {
                compPerson = personRepository.save(compPerson);
                Complainant complainant = complainantMapper.toComplainant(compPerson);
                aCase.setComplainant(complainant);
            }
        }

        // Respondent
        if (request.respondent() != null) {
            Person respPerson = respondentMapper.toNewPerson(request.respondent());
            if (respPerson != null) {
                respPerson = personRepository.save(respPerson);
                Respondent respondent = respondentMapper.toRespondent(respPerson, request.respondent());
                aCase.setRespondent(respondent);
            }
        }

        // Incident Details
        if (request.incident() != null) {
            IncidentDetail incidentDetail = incidentDetailMapper.fromCreateDto(request.incident());
            aCase.setIncidentDetail(incidentDetail);
        }

        // Narrative Statement (Sworn Statement document)
        if (request.narrative() != null) {
            NarrativeStatement narrativeStatement = narrativeStatementMapper.fromCreateDto(request.narrative());
            aCase.setNarrativeStatement(narrativeStatement);
        }

        // Assigned Officer
        if (request.assignedOfficerId() != null) {
            officerRepository.findById(request.assignedOfficerId()).ifPresent(aCase::setOfficer);
        } else if (request.assignedOfficer() != null && !request.assignedOfficer().isBlank()) {
            String searchName = request.assignedOfficer().toLowerCase();
            officerRepository.findAll().stream()
                    .filter(o -> o.getPerson() != null && (
                            (o.getPerson().getFirstName() + " " + o.getPerson().getLastName()).toLowerCase().contains(searchName) ||
                                    searchName.contains(o.getPerson().getLastName().toLowerCase())
                    ))
                    .findFirst()
                    .ifPresent(aCase::setOfficer);
        }

        // Danger Assessment & Risk Level & Immediate Requests
        if (request.dangerAssessments() != null && !request.dangerAssessments().isEmpty()) {
            String joined = String.join("; ", request.dangerAssessments());
            if (request.otherDangerAssessment() != null && !request.otherDangerAssessment().isBlank()) {
                joined += "; Other: " + request.otherDangerAssessment().trim();
            }
            aCase.setDangerAssessments(joined);
        } else if (request.otherDangerAssessment() != null && !request.otherDangerAssessment().isBlank()) {
            aCase.setDangerAssessments("Other: " + request.otherDangerAssessment().trim());
        }

        String computedRisk = calculateRiskLevel(request.dangerAssessments(), request.riskLevel());
        aCase.setRiskLevel(computedRisk);

        if (request.immediateRequests() != null && !request.immediateRequests().isEmpty()) {
            aCase.setImmediateRequests(String.join(", ", request.immediateRequests()));
        }

        // Initial Save Case Entity
        Case savedCase = caseRepository.save(aCase);

        // Children Involved
        if (Boolean.TRUE.equals(request.hasChildrenInvolved()) && request.children() != null && !request.children().isEmpty()) {
            List<ChildInvolved> childrenList = new ArrayList<>();
            for (ChildInvolvedCreateDto cDto : request.children()) {
                ChildInvolved child = new ChildInvolved();
                child.setName(cDto.getFullName());
                child.setAge(cDto.age());
                child.setChildRole(cDto.role() != null ? cDto.role() : "Affected Child");
                child.setCases(savedCase);
                childrenList.add(childInvolvedRepository.save(child));
            }
            savedCase.setChildren(childrenList);
            savedCase = caseRepository.save(savedCase);
        }

        // Witnesses
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

        // Evidences
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

        // Case Timeline Event Logging - Case Filed is logged first
        caseTimelineService.logEvent(
                savedCase,
                TimelineEventType.CASE_FILED,
                "VAWC Case Filed",
                "VAWC Case #" + savedCase.getCaseNumber() + " was filed with Risk Level: " + savedCase.getRiskLevel(),
                currentUser
        );

        // Violence Type & Barangay Protection Order (BPO) Linkage
        boolean bpoRequested = request.immediateRequests() != null && request.immediateRequests().stream()
                .anyMatch(r -> r.toLowerCase().contains("protection order") || r.toLowerCase().contains("bpo"));

        if (bpoRequested) {
            ViolenceType vt = null;
            if (request.violenceTypeId() != null) {
                vt = violenceTypeRepository.findById(request.violenceTypeId()).orElse(null);
            }
            if (vt == null && request.violenceType() != null && !request.violenceType().isBlank()) {
                String vtName = request.violenceType().trim();
                vt = violenceTypeRepository.findByNameIgnoreCase(vtName)
                        .orElseGet(() -> violenceTypeRepository.save(new ViolenceType(null, vtName, "VAWC Violence Category")));
            }
            if (vt == null) {
                vt = violenceTypeRepository.findByNameIgnoreCase("VAWC Protection Order Request")
                        .orElseGet(() -> violenceTypeRepository.save(new ViolenceType(null, "VAWC Protection Order Request", "VAWC Violence Category")));
            }

            BarangayProtectionOrder bpo = new BarangayProtectionOrder();
            bpo.setCases(savedCase);
            bpo.setBpoControlNumber("BPO-" + savedCase.getCaseNumber());
            bpo.setStatus(BpoStatus.ISSUED);
            bpo.setActivatedAt(LocalDateTime.now());
            bpo.setExpiredAt(LocalDate.now().plusDays(15));
            if (vt != null) {
                bpo.getViolenceTypes().add(vt);
            }
            bpo.setCreatedBy(currentUser);
            bpo = barangayProtectionOrderRepository.save(bpo);
            savedCase.setBarangayProtectionOrder(bpo);
            savedCase.setCaseStatus(CaseStatus.BPO_ISSUED);
            savedCase.setCaseStatusRemarks("Barangay Protection Order #" + bpo.getBpoControlNumber() + " issued with 15-day validity upon complaint filing. Pending service to respondent.");
            savedCase = caseRepository.save(savedCase);

            caseTimelineService.logEvent(
                    savedCase,
                    TimelineEventType.BPO_ISSUED,
                    "BPO Issued (15-Day Validity)",
                    "Barangay Protection Order #" + bpo.getBpoControlNumber() + " was automatically issued with 15-day validity upon complaint filing. Awaiting service to respondent.",
                    currentUser
            );
        }

        log.info("VAWC Case #{} successfully created with ID {}", savedCase.getCaseNumber(), savedCase.getId());

        return toDetailResponse(savedCase);
    }

    /**
     * Retrieves paginated and filtered VAWC cases for the records table.
     * Excludes archived, soft-deleted, and permanently deleted cases.
     *
     * @param filter the filter criteria
     * @return {@link PageResponse} containing {@link VawcTableRowResponse}
     */
    @Transactional(readOnly = true)
    public PageResponse<VawcTableRowResponse> getVawcCases(VawcFilterRequest filter) {
        int page = filter != null ? filter.getPageNumber() : 0;
        int size = filter != null ? filter.getPageSize() : 5;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Case> casePage = caseRepository.findVawcCasesFiltered(filter, pageable);

        Page<VawcTableRowResponse> dtoPage = casePage.map(this::toVawcTableRowResponse);
        return PageResponse.of(dtoPage);
    }

    /**
     * Maps a {@link Case} entity to {@link VawcTableRowResponse} for table view.
     */
    public VawcTableRowResponse toVawcTableRowResponse(Case c) {
        if (c == null) return null;

        String compName = "—";
        if (c.getComplainant() != null && c.getComplainant().getPerson() != null) {
            Person p = c.getComplainant().getPerson();
            String f = p.getFirstName() != null ? p.getFirstName() : "";
            String l = p.getLastName() != null ? p.getLastName() : "";
            String m = p.getMiddleName() != null ? p.getMiddleName() : "";
            compName = (l + (l.isBlank() || f.isBlank() ? "" : ", ") + f + (m.isBlank() ? "" : " " + m.charAt(0) + ".")).trim();
        }

        String respName = "—";
        if (c.getRespondent() != null && c.getRespondent().getPerson() != null) {
            Person p = c.getRespondent().getPerson();
            String f = p.getFirstName() != null ? p.getFirstName() : "";
            String l = p.getLastName() != null ? p.getLastName() : "";
            String m = p.getMiddleName() != null ? p.getMiddleName() : "";
            respName = (l + (l.isBlank() || f.isBlank() ? "" : ", ") + f + (m.isBlank() ? "" : " " + m.charAt(0) + ".")).trim();
        }

        String nature = "VAWC Complaint";
        if (c.getIncidentDetail() != null && c.getIncidentDetail().getNatureOfComplaint() != null && !c.getIncidentDetail().getNatureOfComplaint().isBlank()) {
            nature = c.getIncidentDetail().getNatureOfComplaint().trim();
        } else if (c.getBarangayProtectionOrder() != null && c.getBarangayProtectionOrder().getViolenceTypes() != null && !c.getBarangayProtectionOrder().getViolenceTypes().isEmpty()) {
            nature = c.getBarangayProtectionOrder().getViolenceTypes().stream()
                    .map(ViolenceType::getName)
                    .collect(Collectors.joining(", "));
        }

        String dateFiled = c.getCreatedAt() != null
                ? c.getCreatedAt().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
                : "—";

        return new VawcTableRowResponse(
                c.getId(),
                c.getCaseNumber(),
                compName,
                respName,
                nature,
                c.getCaseStatus(),
                c.getRiskLevel() != null ? c.getRiskLevel() : "LOW",
                dateFiled,
                c.getCreatedAt()
        );
    }

    /**
     * Retrieves all VAWC cases (excluding archived, deleted).
     */
    @Transactional(readOnly = true)
    public List<VawcCaseDetailResponse> getAllVawcCases() {
        return caseRepository.findRootVawcCases().stream()
                .map(this::toDetailResponse)
                .toList();
    }

    /**
     * Retrieves a single VAWC case by ID.
     */
    @Transactional(readOnly = true)
    public VawcCaseDetailResponse getVawcCaseById(Long caseId) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("VAWC Case not found with ID: " + caseId));
        return toDetailResponse(aCase);
    }

    /**
     * Updates an existing VAWC Case record differentially (incident details, narrative, evidences, witnesses).
     */
    @Transactional
    public VawcCaseDetailResponse updateVawcCase(Long caseId, VawcCaseUpdateRequest request) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("VAWC Case not found with ID: " + caseId));

        if (aCase.getCaseStatus() == CaseStatus.SETTLED || aCase.getCaseStatus() == CaseStatus.DISMISSED || aCase.getCaseStatus() == CaseStatus.WITHDRAWN || aCase.getCaseStatus() == CaseStatus.CLOSED || aCase.getCaseStatus() == CaseStatus.CERTIFIED_TO_FILE_ACTION || aCase.getCaseStatus() == CaseStatus.EXPIRED_UNACTIONED) {
            throw new IllegalStateException("Cannot edit a settled, dismissed, or closed VAWC case.");
        }

        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            aCase.setCaseUpdatedBy(currentUser);
        }

        // Update Respondent relationship & living status
        if (aCase.getRespondent() != null) {
            if (request.relationshipToComplainant() != null) {
                aCase.getRespondent().setRelationshipToComplainant(request.relationshipToComplainant());
            }
            if (request.currentlyLivingWithComplainant() != null) {
                aCase.getRespondent().setLivingWithComplainant(request.currentlyLivingWithComplainant());
            }
        }

        // Update Incident Details
        if (request.incident() != null) {
            IncidentDetail inc = incidentDetailMapper.fromCreateDto(request.incident());
            aCase.setIncidentDetail(inc);
        }

        // Update Narrative Statement
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

        // Update Evidences (Selective update without losing existing evidence files)
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
                    CaseEvidence newEv = caseEvidenceMapper.fromCreateDto(eDto, aCase);
                    if (newEv != null) {
                        newEv = caseEvidenceRepository.save(newEv);
                        updatedEvidences.add(newEv);
                    }
                }
            }

            for (CaseEvidence ce : existingEvidences) {
                if (!updatedEvidences.contains(ce)) {
                    caseEvidenceRepository.delete(ce);
                }
            }
        }

        // Update Witnesses (Selective in-place update without losing testimony files)
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
                    Person p = targetWitness.getPerson();
                    if (p == null) {
                        p = new Person();
                    }
                    if (wDto.firstName() != null) p.setFirstName(wDto.firstName().trim());
                    if (wDto.lastName() != null) p.setLastName(wDto.lastName().trim());
                    if (wDto.contactNumber() != null) p.setContactNumber(wDto.contactNumber().trim());
                    p = personRepository.save(p);
                    targetWitness.setPerson(p);

                    if (wDto.testimonyFile() != null && wDto.testimonyFile().length > 0) {
                        targetWitness.setTestimonyFile(wDto.testimonyFile());
                        if (wDto.testimonyFileName() != null && !wDto.testimonyFileName().isBlank()) {
                            targetWitness.setTestimonyFileName(wDto.testimonyFileName().trim());
                        }
                    }
                    targetWitness = witnessRepository.save(targetWitness);
                    updatedWitnessEntities.add(targetWitness);
                } else {
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

        // Danger assessments / violenceType
        if (request.dangerAssessments() != null && !request.dangerAssessments().isEmpty()) {
            String joined = String.join("; ", request.dangerAssessments());
            if (request.otherDangerAssessment() != null && !request.otherDangerAssessment().isBlank()) {
                joined += "; Other: " + request.otherDangerAssessment().trim();
            }
            aCase.setDangerAssessments(joined);
        }
        if (request.riskLevel() != null && !request.riskLevel().isBlank()) {
            aCase.setRiskLevel(request.riskLevel().toUpperCase().trim());
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

        return toDetailResponse(updatedCase);
    }

    /**
     * Updates the status of a VAWC case (e.g. SETTLED, DISMISSED) with optional status remarks.
     *
     * @param id the VAWC case ID
     * @param request the status update request payload containing status and remarks
     * @return updated {@link VawcCaseDetailResponse}
     */
    @Transactional
    public VawcCaseDetailResponse updateCaseStatus(Long id, UpdateCaseStatusRequest request) {
        Case aCase = caseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VAWC case not found with id: " + id));

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

        // Persist Case Entity
        Case updated = caseRepository.save(aCase);

        // Log Timeline Event
        try {
            String remarksText = request.getRemarks() != null ? request.getRemarks().trim() : "";
            TimelineEventType eventType = TimelineEventType.STATUS_CHANGED;
            String eventTitle;
            String detailsText;

            if (newStatus == CaseStatus.SETTLED) {
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
        } catch (Exception ex) {
            log.warn("Failed to log timeline event for status update [caseId={}]: {}", updated.getId(), ex.getMessage());
        }

        return toDetailResponse(updated);
    }

    /**
     * Calculates VAWC KPI Statistics with month-to-date vs full previous calendar month trend percentage.
     *
     * @return {@link VawcStatsResponse}
     */
    @Transactional(readOnly = true)
    public VawcStatsResponse getVawcStats() {
        LocalDateTime currentMonthStart = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime prevMonthStart = YearMonth.now().minusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime prevMonthEnd = currentMonthStart.minusNanos(1);

        // Total VAWC Cases & Monthly Trend
        long totalCases = caseRepository.countRootVawcCases();
        long currentTotal = caseRepository.countRootVawcCasesByCreatedAtGreaterThanEqual(currentMonthStart);
        long prevTotal = caseRepository.countRootVawcCasesByCreatedAtBetween(prevMonthStart, prevMonthEnd);
        String totalCasesTrend = calculateMonthlyTrend(currentTotal, prevTotal);

        // Total Referred Cases
        long totalReferredCases = caseRepository.countVawcCasesByCaseStatusIn(List.of(CaseStatus.ISSUED_REFERRAL));

        // Total Active BPO (Issued, Served) - Excludes PENDING applications
        List<BpoStatus> activeBpoStatuses = List.of(BpoStatus.ISSUED, BpoStatus.SERVED);
        long totalActiveBpo = barangayProtectionOrderRepository.countByStatusIn(activeBpoStatuses);

        // Total Withdrawn Cases
        long totalWithdrawnCases = caseRepository.countVawcCasesByCaseStatusIn(List.of(CaseStatus.WITHDRAWN));

        return new VawcStatsResponse(
                totalCases,
                totalCasesTrend,
                totalReferredCases,
                totalActiveBpo,
                totalWithdrawnCases
        );
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
     * Maps a {@link Case} entity to {@link VawcCaseDetailResponse}.
     */
    public VawcCaseDetailResponse toDetailResponse(Case c) {
        if (c == null) return null;

        String assignedOfficerName = null;
        Long assignedOfficerId = null;
        if (c.getOfficer() != null) {
            assignedOfficerId = c.getOfficer().getId();
            if (c.getOfficer().getPerson() != null) {
                assignedOfficerName = (c.getOfficer().getPerson().getFirstName() + " " + c.getOfficer().getPerson().getLastName()).trim();
            }
        }

        List<ChildInvolvedDetailDto> childDtos = new ArrayList<>();
        if (c.getChildren() != null) {
            childDtos = c.getChildren().stream()
                    .map(child -> new ChildInvolvedDetailDto(child.getId(), child.getName(), child.getAge(), child.getChildRole()))
                    .toList();
        }

        List<CaseEvidence> evidenceList = caseEvidenceRepository.findByCasesId(c.getId());
        EvidenceDetailDto singleEvidence = (evidenceList != null && !evidenceList.isEmpty()) ? caseEvidenceMapper.toDetailDto(evidenceList.get(0)) : null;
        List<WitnessDetailDto> witnessDtos = (c.getWitnesses() != null)
                ? c.getWitnesses().stream().map(witnessMapper::toDetailDto).toList()
                : List.of();

        String violenceTypeName = null;
        if (c.getBarangayProtectionOrder() != null && c.getBarangayProtectionOrder().getViolenceTypes() != null && !c.getBarangayProtectionOrder().getViolenceTypes().isEmpty()) {
            violenceTypeName = c.getBarangayProtectionOrder().getViolenceTypes().iterator().next().getName();
        } else if (c.getIncidentDetail() != null && c.getIncidentDetail().getNatureOfComplaint() != null && !c.getIncidentDetail().getNatureOfComplaint().isBlank()) {
            violenceTypeName = c.getIncidentDetail().getNatureOfComplaint().trim();
        }

        BpoDetailDto bpoDto = null;
        BarangayProtectionOrder bpo = c.getBarangayProtectionOrder();
        if (bpo == null && c.getId() != null) {
            bpo = barangayProtectionOrderRepository.findByCasesId(c.getId()).orElse(null);
        }
        if (bpo != null) {
            LocalDateTime activatedAt = bpo.getActivatedAt();
            if (activatedAt == null && (bpo.getStatus() == BpoStatus.ISSUED || bpo.getStatus() == BpoStatus.SERVED || c.getCaseStatus() == CaseStatus.BPO_ISSUED || c.getCaseStatus() == CaseStatus.BPO_SERVED)) {
                activatedAt = bpo.getDateServed() != null ? bpo.getDateServed() : (bpo.getCreatedAt() != null ? bpo.getCreatedAt() : c.getCreatedAt());
            }
            LocalDate expiredAt = bpo.getExpiredAt();
            if (expiredAt == null && activatedAt != null) {
                expiredAt = activatedAt.toLocalDate().plusDays(15);
            }

            bpoDto = new BpoDetailDto(
                    bpo.getId(),
                    bpo.getBpoControlNumber(),
                    bpo.getStatus(),
                    activatedAt,
                    expiredAt,
                    bpo.getServedBy(),
                    bpo.getDateServed(),
                    bpo.getServiceMethod(),
                    bpo.getWitness1Name(),
                    bpo.getWitness2Name(),
                    null,
                    bpo.getCertificationOfServiceFileData()
            );
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a");
        String dateFiledFormatted = c.getCreatedAt() != null ? c.getCreatedAt().format(dtf) : "—";

        return new VawcCaseDetailResponse(
                c.getId(),
                c.getCaseNumber(),
                c.getFilingType(),
                c.getCaseStatus(),
                c.getCaseStatusRemarks(),
                dateFiledFormatted,
                assignedOfficerId,
                assignedOfficerName,
                complainantMapper.toDetailDto(c.getComplainant()),
                respondentMapper.toDetailDto(c.getRespondent()),
                incidentDetailMapper.toDetailDto(c.getIncidentDetail()),
                violenceTypeName,
                narrativeStatementMapper.toDetailDto(c.getNarrativeStatement()),
                singleEvidence,
                caseEvidenceMapper.toDetailDtoList(evidenceList),
                witnessDtos,
                childDtos,
                c.getRiskLevel(),
                c.getDangerAssessments(),
                c.getImmediateRequests(),
                bpoDto,
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    /**
     * Activates the Barangay Protection Order (BPO) for a given VAWC case.
     * Sets status to ISSUED, activates validity period (15 days), and logs timeline event.
     *
     * @param caseId the VAWC case ID
     * @return {@link VawcCaseDetailResponse} with updated BPO state
     */
    @Transactional
    public VawcCaseDetailResponse activateBpo(Long caseId) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("VAWC Case not found with ID: " + caseId));

        if (aCase.getCaseStatus() == CaseStatus.SETTLED || aCase.getCaseStatus() == CaseStatus.DISMISSED || aCase.getCaseStatus() == CaseStatus.WITHDRAWN || aCase.getCaseStatus() == CaseStatus.CLOSED || aCase.getCaseStatus() == CaseStatus.CERTIFIED_TO_FILE_ACTION || aCase.getCaseStatus() == CaseStatus.EXPIRED_UNACTIONED) {
            throw new IllegalStateException("Cannot edit a settled, dismissed, or closed VAWC case.");
        }
        final Case targetCase = aCase;

        BarangayProtectionOrder bpo = barangayProtectionOrderRepository.findByCasesId(caseId)
                .orElseGet(() -> {
                    BarangayProtectionOrder newBpo = new BarangayProtectionOrder();
                    newBpo.setCases(targetCase);
                    return newBpo;
                });

        if (bpo.getBpoControlNumber() == null || bpo.getBpoControlNumber().isBlank()) {
            bpo.setBpoControlNumber("BPO-VAWC-" + targetCase.getCaseNumber().replace("VAWC-", ""));
        }

        bpo.setStatus(BpoStatus.ISSUED);
        bpo.setActivatedAt(LocalDateTime.now());
        bpo.setExpiredAt(LocalDate.now().plusDays(15));

        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null && bpo.getCreatedBy() == null) {
            bpo.setCreatedBy(currentUser);
        }

        bpo = barangayProtectionOrderRepository.save(bpo);
        targetCase.setBarangayProtectionOrder(bpo);
        targetCase.setCaseStatus(CaseStatus.BPO_ISSUED);
        targetCase.setCaseStatusRemarks("Barangay Protection Order #" + bpo.getBpoControlNumber() + " issued by Punong Barangay.");
        Case savedCase = caseRepository.save(targetCase);

        caseTimelineService.logEvent(
                savedCase,
                TimelineEventType.BPO_ISSUED,
                "BPO Activated",
                "Barangay Protection Order #" + bpo.getBpoControlNumber() + " was officially activated and issued with 15-day validity.",
                currentUser
        );

        log.info("BPO for VAWC case #{} activated successfully. Control No: {}", savedCase.getCaseNumber(), bpo.getBpoControlNumber());
        return toDetailResponse(savedCase);
    }

    /**
     * Records the service of the Barangay Protection Order (BPO) to the respondent.
     * Updates servedBy, dateServed, serviceMethod, and status to SERVED.
     *
     * @param caseId the VAWC case ID
     * @param request the BPO service details
     * @return {@link VawcCaseDetailResponse} with updated BPO state
     */
    @Transactional
    public VawcCaseDetailResponse serveBpo(Long caseId, BpoServeRequest request) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("VAWC Case not found with ID: " + caseId));

        if (aCase.getCaseStatus() == CaseStatus.SETTLED || aCase.getCaseStatus() == CaseStatus.DISMISSED || aCase.getCaseStatus() == CaseStatus.WITHDRAWN || aCase.getCaseStatus() == CaseStatus.CLOSED || aCase.getCaseStatus() == CaseStatus.CERTIFIED_TO_FILE_ACTION || aCase.getCaseStatus() == CaseStatus.EXPIRED_UNACTIONED) {
            throw new IllegalStateException("Cannot edit a settled, dismissed, or closed VAWC case.");
        }
        final Case targetCase = aCase;

        BarangayProtectionOrder bpo = barangayProtectionOrderRepository.findByCasesId(caseId)
                .orElseGet(() -> {
                    BarangayProtectionOrder newBpo = new BarangayProtectionOrder();
                    newBpo.setCases(targetCase);
                    newBpo.setBpoControlNumber("BPO-VAWC-" + targetCase.getCaseNumber().replace("VAWC-", ""));
                    newBpo.setActivatedAt(LocalDateTime.now());
                    newBpo.setExpiredAt(LocalDate.now().plusDays(15));
                    return newBpo;
                });

        if (bpo.getActivatedAt() == null) {
            bpo.setActivatedAt(bpo.getDateServed() != null ? bpo.getDateServed() : LocalDateTime.now());
        }
        if (bpo.getExpiredAt() == null) {
            bpo.setExpiredAt(bpo.getActivatedAt().toLocalDate().plusDays(15));
        }

        bpo.setServedBy(request.servedBy());
        bpo.setDateServed(request.dateServed() != null ? request.dateServed() : LocalDateTime.now());
        bpo.setServiceMethod(request.serviceMethod());
        if (request.witness1Name() != null && !request.witness1Name().isBlank()) {
            bpo.setWitness1Name(request.witness1Name().trim());
        }
        if (request.witness2Name() != null && !request.witness2Name().isBlank()) {
            bpo.setWitness2Name(request.witness2Name().trim());
        }
        if (request.certificationFileData() != null && !request.certificationFileData().isBlank()) {
            bpo.setCertificationOfServiceFileData(request.certificationFileData());
        }
        bpo.setStatus(BpoStatus.SERVED);

        User currentUser = SecurityUtils.getCurrentUser();
        bpo = barangayProtectionOrderRepository.save(bpo);
        targetCase.setBarangayProtectionOrder(bpo);
        targetCase.setCaseStatus(CaseStatus.BPO_SERVED);
        targetCase.setCaseStatusRemarks("Barangay Protection Order #" + bpo.getBpoControlNumber() + " served to respondent by " + request.servedBy() + " via " + request.serviceMethod() + ".");
        Case savedCase = caseRepository.save(targetCase);

        caseTimelineService.logEvent(
                savedCase,
                TimelineEventType.STATUS_CHANGED,
                "BPO Served to Respondent",
                "Barangay Protection Order #" + bpo.getBpoControlNumber() + " was served by " + request.servedBy() + " via " + request.serviceMethod() + ".",
                currentUser
        );

        log.info("BPO for VAWC case #{} marked as served by {}", savedCase.getCaseNumber(), request.servedBy());
        return toDetailResponse(savedCase);
    }

    /**
     * Issues a new formal referral for a VAWC case to a partner agency (DSWD, PNP, PAO, Medical, etc.).
     *
     * @param caseId the VAWC case ID
     * @param request referral creation payload
     * @return created {@link VawcReferralResponse}
     */
    @Transactional
    public VawcReferralResponse createReferral(Long caseId, VawcReferralCreateRequest request) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("VAWC Case not found with ID: " + caseId));

        if (aCase.getCaseStatus() == CaseStatus.SETTLED || aCase.getCaseStatus() == CaseStatus.DISMISSED || aCase.getCaseStatus() == CaseStatus.WITHDRAWN || aCase.getCaseStatus() == CaseStatus.CLOSED || aCase.getCaseStatus() == CaseStatus.CERTIFIED_TO_FILE_ACTION || aCase.getCaseStatus() == CaseStatus.EXPIRED_UNACTIONED) {
            throw new IllegalStateException("Cannot edit a settled, dismissed, or closed VAWC case.");
        }

        long existingCount = vawcReferralRepository.findByCasesId(caseId).size();
        String referralControlNumber = String.format("REF-%s-%02d", aCase.getCaseNumber(), existingCount + 1);

        VawcReferral referral = new VawcReferral();
        referral.setCases(aCase);
        referral.setReferralControlNumber(referralControlNumber);
        referral.setReferredTo(request.referredTo().trim());
        referral.setSubjectOfLitigations(request.subjectOfLitigations() != null ? request.subjectOfLitigations().trim() : null);
        referral.setGrounds(request.grounds() != null ? request.grounds().trim() : null);
        referral.setReferredBy(request.referredBy() != null ? request.referredBy().trim() : "VAWC Desk Officer");
        referral.setReferralDate(request.referralDate() != null ? request.referralDate() : LocalDateTime.now());
        referral.setStatus("Referral Pending");

        referral = vawcReferralRepository.save(referral);

        // Update case status to ISSUED_REFERRAL
        aCase.setCaseStatus(CaseStatus.ISSUED_REFERRAL);
        aCase.setCaseStatusRemarks("Referral pending endorsement / feedback from " + referral.getReferredTo() + " under Referral #" + referralControlNumber);
        caseRepository.save(aCase);

        User currentUser = SecurityUtils.getCurrentUser();
        caseTimelineService.logEvent(
                aCase,
                TimelineEventType.STATUS_CHANGED,
                "Case Referral Pending (" + referral.getReferredTo() + ")",
                "Referral #" + referralControlNumber + " was officially issued to " + referral.getReferredTo() + " by " + referral.getReferredBy() + ". Awaiting agency feedback.",
                currentUser
        );

        // Close/Terminate BPO upon inter-agency referral escalation
        BarangayProtectionOrder bpo = aCase.getBarangayProtectionOrder();
        if (bpo == null) {
            bpo = barangayProtectionOrderRepository.findByCasesId(caseId).orElse(null);
        }
        if (bpo != null && (bpo.getStatus() == BpoStatus.ISSUED || bpo.getStatus() == BpoStatus.SERVED || bpo.getStatus() == BpoStatus.PENDING)) {
            bpo.setStatus(BpoStatus.EXPIRED);
            barangayProtectionOrderRepository.save(bpo);
            aCase.setBarangayProtectionOrder(bpo);
            log.info("BPO #{} marked as EXPIRED upon referral of VAWC Case #{}", bpo.getBpoControlNumber(), aCase.getCaseNumber());
        }

        log.info("VAWC Case #{} referred to {} with Control #{}", aCase.getCaseNumber(), referral.getReferredTo(), referralControlNumber);
        return toReferralResponse(referral, null);
    }

    /**
     * Retrieves all referrals for a VAWC case with their feedback history.
     *
     * @param caseId the VAWC case ID
     * @return list of {@link VawcReferralResponse}
     */
    @Transactional(readOnly = true)
    public List<VawcReferralResponse> getReferralsByCaseId(Long caseId) {
        if (!caseRepository.existsById(caseId)) {
            throw new ResourceNotFoundException("VAWC Case not found with ID: " + caseId);
        }
        List<VawcReferral> referrals = vawcReferralRepository.findByCasesIdOrderByIdDesc(caseId);
        return referrals.stream()
                .map(r -> {
                    ReferralFeedback feedback = referralFeedbackRepository.findByVawcReferralId(r.getId()).orElse(null);
                    return toReferralResponse(r, feedback);
                })
                .collect(Collectors.toList());
    }

    /**
     * Records external agency feedback on a VAWC referral.
     *
     * @param caseId the VAWC case ID
     * @param referralId the referral ID
     * @param request feedback payload
     * @return updated {@link VawcReferralResponse}
     */
    @Transactional
    public VawcReferralResponse addReferralFeedback(Long caseId, Long referralId, ReferralFeedbackRequest request) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("VAWC Case not found with ID: " + caseId));

        if (aCase.getCaseStatus() == CaseStatus.SETTLED || aCase.getCaseStatus() == CaseStatus.DISMISSED || aCase.getCaseStatus() == CaseStatus.WITHDRAWN || aCase.getCaseStatus() == CaseStatus.CLOSED || aCase.getCaseStatus() == CaseStatus.CERTIFIED_TO_FILE_ACTION || aCase.getCaseStatus() == CaseStatus.EXPIRED_UNACTIONED) {
            throw new IllegalStateException("Cannot edit a settled, dismissed, or closed VAWC case.");
        }

        VawcReferral referral = vawcReferralRepository.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("VAWC Referral not found with ID: " + referralId));

        ReferralFeedback feedback = referralFeedbackRepository.findByVawcReferralId(referralId)
                .orElseGet(() -> {
                    ReferralFeedback newFb = new ReferralFeedback();
                    newFb.setCases(aCase);
                    newFb.setVawcReferral(referral);
                    return newFb;
                });

        feedback.setDetails(request.details().trim());
        feedback.setResolvesCase(request.resolvesCase() != null && request.resolvesCase());
        feedback.setResolutionType(request.resolutionType());
        if (request.proofFileData() != null && !request.proofFileData().isBlank()) {
            feedback.setProofFileData(request.proofFileData());
            feedback.setProofFileName(request.proofFileName() != null ? request.proofFileName() : "referral_feedback.pdf");
        }
        feedback.setDeclinedByAgency(request.declinedByAgency() != null && request.declinedByAgency());
        feedback.setReasonForDecline(request.reasonForDecline());

        ReferralFeedback savedFeedback = referralFeedbackRepository.save(feedback);

        User currentUser = SecurityUtils.getCurrentUser();

        if (Boolean.TRUE.equals(savedFeedback.getDeclinedByAgency())) {
            referral.setStatus("Declined");
            aCase.setCaseStatus(CaseStatus.DECLINED_REFERRAL);
            String declineReason = savedFeedback.getReasonForDecline() != null && !savedFeedback.getReasonForDecline().isBlank()
                    ? savedFeedback.getReasonForDecline().trim()
                    : savedFeedback.getDetails();
            aCase.setCaseStatusRemarks("Referral declined by " + referral.getReferredTo() + ": " + declineReason);
            caseRepository.save(aCase);

            caseTimelineService.logEvent(
                    aCase,
                    TimelineEventType.STATUS_CHANGED,
                    "Referral Declined (" + referral.getReferredTo() + ")",
                    "Referral #" + referral.getReferralControlNumber() + " was declined by " + referral.getReferredTo() + ". Reason: " + declineReason,
                    currentUser
            );
        } else if (Boolean.TRUE.equals(savedFeedback.getResolvesCase())) {
            referral.setStatus("Resolved");
            aCase.setCaseStatus(CaseStatus.SETTLED);
            aCase.setCaseStatusRemarks("Case resolved following referral to " + referral.getReferredTo() + ": " + savedFeedback.getDetails());
            caseRepository.save(aCase);

            caseTimelineService.logEvent(
                    aCase,
                    TimelineEventType.STATUS_CHANGED,
                    "Case Resolved via Referral (" + referral.getReferredTo() + ")",
                    "Case settled and resolved following successful referral endorsement to " + referral.getReferredTo() + ": " + savedFeedback.getDetails(),
                    currentUser
            );
        } else {
            referral.setStatus("Referred");
            aCase.setCaseStatus(CaseStatus.ISSUED_REFERRAL);
            aCase.setCaseStatusRemarks("Referral completed and feedback recorded from " + referral.getReferredTo() + ": " + savedFeedback.getDetails());
            caseRepository.save(aCase);

            caseTimelineService.logEvent(
                    aCase,
                    TimelineEventType.STATUS_CHANGED,
                    "Referral Feedback Recorded (" + referral.getReferredTo() + ")",
                    "Feedback recorded for Referral #" + referral.getReferralControlNumber() + ": " + savedFeedback.getDetails(),
                    currentUser
            );
        }
        VawcReferral updatedReferral = vawcReferralRepository.save(referral);

        log.info("Feedback recorded for VAWC referral #{} with Case Status {}", updatedReferral.getReferralControlNumber(), aCase.getCaseStatus());
        return toReferralResponse(updatedReferral, savedFeedback);
    }

    private VawcReferralResponse toReferralResponse(VawcReferral referral, ReferralFeedback feedback) {
        ReferralFeedbackDto feedbackDto = feedback != null ? new ReferralFeedbackDto(
                feedback.getId(),
                feedback.getDetails(),
                feedback.getResolvesCase(),
                feedback.getResolutionType(),
                feedback.getProofFileName(),
                feedback.getProofFileData(),
                feedback.getDeclinedByAgency(),
                feedback.getReasonForDecline(),
                feedback.getCreatedAt()
        ) : null;

        return new VawcReferralResponse(
                referral.getId(),
                referral.getCases() != null ? referral.getCases().getId() : null,
                referral.getCases() != null ? referral.getCases().getCaseNumber() : null,
                referral.getReferralControlNumber(),
                referral.getReferredTo(),
                referral.getSubjectOfLitigations(),
                referral.getGrounds(),
                referral.getStatus(),
                referral.getReferredBy(),
                referral.getReferralDate(),
                feedbackDto,
                referral.getCreatedAt()
        );
    }

    /**
     * Retrieves all timeline events for a VAWC case.
     *
     * @param caseId the VAWC case ID
     * @return list of {@link TimelineEventResponse}
     */
    @Transactional(readOnly = true)
    public List<TimelineEventResponse> getCaseTimeline(Long caseId) {
        return caseTimelineService.getTimelineForCase(caseId);
    }

    /**
     * Generates a unique VAWC case number in the format: YYYY-VAWC-XXX (e.g. 2026-VAWC-001).
     */
    private String generateUniqueVawcCaseNumber() {
        int year = LocalDateTime.now().getYear();
        for (int i = 0; i < 100; i++) {
            long count = caseRepository.count() + 1 + i;
            String candidate = String.format("%d-VAWC-%03d", year, count);
            if (!caseRepository.existsByCaseNumber(candidate)) {
                return candidate;
            }
        }
        return String.format("%d-VAWC-%04d", year, RANDOM.nextInt(9000) + 1000);
    }

    /**
     * Calculates the Risk Level based on checked danger assessment factors.
     */
    private String calculateRiskLevel(List<String> factors, String providedRiskLevel) {
        if (providedRiskLevel != null && !providedRiskLevel.isBlank()) {
            return providedRiskLevel.toUpperCase().trim();
        }
        if (factors == null || factors.isEmpty()) {
            return "LOW";
        }

        // Critical indicators that immediately trigger HIGH-IMMINENT
        boolean hasWeapons = factors.stream().anyMatch(f -> f.toLowerCase().contains("weapon") || f.toLowerCase().contains("dangerous object"));
        boolean hasOngoingThreat = factors.stream().anyMatch(f -> f.toLowerCase().contains("ongoing") || f.toLowerCase().contains("continuing threat"));
        boolean hasEscalation = factors.stream().anyMatch(f -> f.toLowerCase().contains("escalating"));

        if (hasWeapons || (hasOngoingThreat && factors.size() >= 2)) {
            return "HIGH-IMMINENT";
        }

        int score = factors.size();
        if (score >= 4 || hasEscalation) {
            return "HIGH-IMMINENT";
        } else if (score >= 2) {
            return "MODERATE";
        } else {
            return "LOW";
        }
    }
}
