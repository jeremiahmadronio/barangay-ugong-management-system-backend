package com.barangay.cases.vawc.service;

import com.barangay.cases.shared.model.*;
import com.barangay.cases.shared.repository.CaseEvidenceRepository;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.cases.vawc.dto.*;
import com.barangay.cases.vawc.model.BarangayProtectionOrder;
import com.barangay.cases.vawc.model.ChildInvolved;
import com.barangay.cases.vawc.model.ViolenceType;
import com.barangay.cases.vawc.repository.BarangayProtectionOrderRepository;
import com.barangay.cases.vawc.repository.ChildInvolvedRepository;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        // 1. Filing Type & Case Number
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

        // 2. Department & Logged-in User
        departmentRepository.findByName("VAWC").ifPresent(aCase::setDepartment);
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            aCase.setCaseFilledBy(currentUser);
        }

        // 3. Complainant
        if (request.complainant() != null) {
            Person compPerson = complainantMapper.toNewPerson(request.complainant());
            if (compPerson != null) {
                compPerson = personRepository.save(compPerson);
                Complainant complainant = complainantMapper.toComplainant(compPerson);
                aCase.setComplainant(complainant);
            }
        }

        // 4. Respondent
        if (request.respondent() != null) {
            Person respPerson = respondentMapper.toNewPerson(request.respondent());
            if (respPerson != null) {
                respPerson = personRepository.save(respPerson);
                Respondent respondent = respondentMapper.toRespondent(respPerson, request.respondent());
                aCase.setRespondent(respondent);
            }
        }

        // 5. Incident Details
        if (request.incident() != null) {
            IncidentDetail incidentDetail = incidentDetailMapper.fromCreateDto(request.incident());
            aCase.setIncidentDetail(incidentDetail);
        }

        // 6. Narrative Statement (Sworn Statement document)
        if (request.narrative() != null) {
            NarrativeStatement narrativeStatement = narrativeStatementMapper.fromCreateDto(request.narrative());
            aCase.setNarrativeStatement(narrativeStatement);
        }

        // 7. Assigned Officer
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

        // 8. Danger Assessment & Risk Level & Immediate Requests
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

        // 9. Initial Save Case Entity
        Case savedCase = caseRepository.save(aCase);

        // 10. Children Involved
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

        // 11. Witnesses
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

        // 12. Evidences
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

        // 13. Violence Type & Barangay Protection Order (BPO) Linkage
        boolean bpoRequested = request.immediateRequests() != null && request.immediateRequests().stream()
                .anyMatch(r -> r.toLowerCase().contains("protection order") || r.toLowerCase().contains("bpo"));

        if (bpoRequested && ((request.violenceType() != null && !request.violenceType().isBlank()) || request.violenceTypeId() != null)) {
            ViolenceType vt = null;
            if (request.violenceTypeId() != null) {
                vt = violenceTypeRepository.findById(request.violenceTypeId()).orElse(null);
            }
            if (vt == null && request.violenceType() != null && !request.violenceType().isBlank()) {
                String vtName = request.violenceType().trim();
                vt = violenceTypeRepository.findByNameIgnoreCase(vtName)
                        .orElseGet(() -> violenceTypeRepository.save(new ViolenceType(null, vtName, "VAWC Violence Category")));
            }
            if (vt != null) {
                BarangayProtectionOrder bpo = new BarangayProtectionOrder();
                bpo.setCases(savedCase);
                bpo.setBpoControlNumber("BPO-" + savedCase.getCaseNumber());
                bpo.setStatus(BpoStatus.PENDING);
                bpo.getViolenceTypes().add(vt);
                bpo.setCreatedBy(currentUser);
                bpo = barangayProtectionOrderRepository.save(bpo);
                savedCase.setBarangayProtectionOrder(bpo);
            }
        }

        // 14. Case Timeline Event Logging
        caseTimelineService.logEvent(
                savedCase,
                TimelineEventType.CASE_FILED,
                "VAWC Case Filed",
                "VAWC Case #" + savedCase.getCaseNumber() + " was filed with Risk Level: " + savedCase.getRiskLevel(),
                currentUser
        );

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
     * Calculates VAWC KPI Statistics with month-to-date vs full previous calendar month trend percentage.
     *
     * @return {@link VawcStatsResponse}
     */
    @Transactional(readOnly = true)
    public VawcStatsResponse getVawcStats() {
        LocalDateTime currentMonthStart = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime prevMonthStart = YearMonth.now().minusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime prevMonthEnd = currentMonthStart.minusNanos(1);

        // 1. Total VAWC Cases & Monthly Trend
        long totalCases = caseRepository.countRootVawcCases();
        long currentTotal = caseRepository.countRootVawcCasesByCreatedAtGreaterThanEqual(currentMonthStart);
        long prevTotal = caseRepository.countRootVawcCasesByCreatedAtBetween(prevMonthStart, prevMonthEnd);
        String totalCasesTrend = calculateMonthlyTrend(currentTotal, prevTotal);

        // 2. Total Referred Cases
        long totalReferredCases = caseRepository.countVawcCasesByCaseStatusIn(List.of(CaseStatus.ISSUED_REFERRAL));

        // 3. Total Active BPO (Issued, Served) - Excludes PENDING applications
        List<BpoStatus> activeBpoStatuses = List.of(BpoStatus.ISSUED, BpoStatus.SERVED);
        long totalActiveBpo = barangayProtectionOrderRepository.countByStatusIn(activeBpoStatuses);

        // 4. Total Withdrawn Cases
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
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    /**
     * Retrieves all timeline events for a VAWC case.
     *
     * @param caseId the VAWC case ID
     * @return list of {@link com.barangay.cases.blotter.dto.TimelineEventResponse}
     */
    @Transactional(readOnly = true)
    public List<com.barangay.cases.blotter.dto.TimelineEventResponse> getCaseTimeline(Long caseId) {
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
