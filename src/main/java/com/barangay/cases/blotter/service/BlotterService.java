package com.barangay.cases.blotter.service;

import com.barangay.cases.blotter.dto.BlotterCaseCreateRequest;
import com.barangay.cases.blotter.dto.BlotterCaseDetailResponse;
import com.barangay.cases.blotter.dto.BlotterFilterRequest;
import com.barangay.cases.blotter.dto.BlotterStatsResponse;
import com.barangay.cases.sharedmodel.*;
import com.barangay.cases.sharedmodel.repository.CaseEvidenceRepository;
import com.barangay.cases.sharedmodel.repository.CaseRepository;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.people.witness.model.Witness;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.dto.*;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.mapper.*;
import com.barangay.zshared.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic service for managing Blotter Cases (Formal Complaint and For the Record).
 */
@Service
@RequiredArgsConstructor
public class BlotterService {

    private final CaseRepository caseRepository;
    private final PersonRepository personRepository;
    private final OfficerRepository officerRepository;
    private final DepartmentRepository departmentRepository;
    private final CaseEvidenceRepository caseEvidenceRepository;

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
    public PageResponse<BlotterCaseDetailResponse> getBlotterCases(BlotterFilterRequest filter) {
        int page = filter != null ? filter.getPageNumber() : 0;
        int size = filter != null ? filter.getPageSize() : 10;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Case> casePage = caseRepository.findBlotterCasesFiltered(filter, pageable);

        Page<BlotterCaseDetailResponse> dtoPage = casePage.map(c -> toDetailResponse(c, null, null));
        return PageResponse.of(dtoPage);
    }

    /**
     * Retrieves single blotter case details by ID.
     *
     * @param id the case ID
     * @return {@link BlotterCaseDetailResponse}
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

        // 1. Total Cases
        long totalCases = caseRepository.count();
        long currentTotal = caseRepository.countByCreatedAtGreaterThanEqual(currentMonthStart);
        long prevTotal = caseRepository.countByCreatedAtBetween(prevMonthStart, prevMonthEnd);
        String totalCasesTrend = calculateMonthlyTrend(currentTotal, prevTotal);

        // 2. Total For the Record
        long totalForTheRecord = caseRepository.countByFilingType(FilingType.FOR_RECORD_ONLY);
        long currentForRecord = caseRepository.countByFilingTypeAndCreatedAtGreaterThanEqual(FilingType.FOR_RECORD_ONLY, currentMonthStart);
        long prevForRecord = caseRepository.countByFilingTypeAndCreatedAtBetween(FilingType.FOR_RECORD_ONLY, prevMonthStart, prevMonthEnd);
        String totalForTheRecordTrend = calculateMonthlyTrend(currentForRecord, prevForRecord);

        // 3. Under Mediation / In Progress
        List<CaseStatus> activeStatuses = List.of(CaseStatus.PENDING, CaseStatus.UNDER_MEDIATION, CaseStatus.UNDER_CONCILIATION);
        long inProgressCases = caseRepository.countByCaseStatusIn(activeStatuses);

        // 4. Settled / Resolved Cases
        List<CaseStatus> resolvedStatuses = List.of(CaseStatus.SETTLED);
        long resolvedCases = caseRepository.countByCaseStatusIn(resolvedStatuses);
        long currentResolved = caseRepository.countByCaseStatusInAndCreatedAtGreaterThanEqual(resolvedStatuses, currentMonthStart);
        long prevResolved = caseRepository.countByCaseStatusInAndCreatedAtBetween(resolvedStatuses, prevMonthStart, prevMonthEnd);
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
        aCase.setCaseStatus(CaseStatus.PENDING);
        aCase.setIsArchived(false);
        aCase.setIsAttested(request.isCertified() != null ? request.isCertified() : false);

        // Auto-assign Blotter Department
        departmentRepository.findByName("BLOTTER").ifPresent(aCase::setDepartment);

        // Auto-assign caseFiledBy from logged-in security context
        User currentUser = getCurrentUser();
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
                if (wPerson != null) {
                    wPerson = personRepository.save(wPerson);
                    Witness witness = witnessMapper.toWitness(wPerson, savedCase, wDto);
                    witnessEntities.add(witness);
                }
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
        return toDetailResponse(savedCase, request.receivingOfficer(), request.assignedOfficer());
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
     * Maps a saved {@link Case} entity to {@link BlotterCaseDetailResponse}.
     */
    private BlotterCaseDetailResponse toDetailResponse(Case aCase, String receivingOfficerName, String assignedOfficerName) {
        ComplainantDetailDto complainantDto = complainantMapper.toDetailDto(aCase.getComplainant());
        RespondentDetailDto respondentDto = respondentMapper.toDetailDto(aCase.getRespondent());
        IncidentDetailDto incidentDto = incidentDetailMapper.toDetailDto(aCase.getIncidentDetail());
        NarrativeDetailDto narrativeDto = narrativeStatementMapper.toDetailDto(aCase.getNarrativeStatement());

        List<CaseEvidence> evidenceList = caseEvidenceRepository.findByCasesId(aCase.getId());
        EvidenceDetailDto evidenceDto = (evidenceList != null && !evidenceList.isEmpty()) ?
                caseEvidenceMapper.toDetailDto(evidenceList.get(0)) : null;

        List<WitnessDetailDto> witnessDtos = aCase.getWitnesses() != null ?
                aCase.getWitnesses().stream().map(witnessMapper::toDetailDto).toList() : List.of();

        String formattedDate = aCase.getCreatedAt() != null ?
                aCase.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) :
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

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
                aCase.getCaseStatus(),
                aCase.getCaseNumber(),
                formattedDate,
                null,
                receivingOfficerName,
                assignedId,
                assignedName,
                complainantDto,
                respondentDto,
                incidentDto,
                narrativeDto,
                evidenceDto,
                witnessDtos,
                aCase.getIsAttested(),
                aCase.getCreatedAt(),
                aCase.getUpdatedAt()
        );
    }

    /**
     * Extracts the logged-in User entity from Spring SecurityContextHolder.
     */
    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                return userDetails.user();
            }
        } catch (Exception ignored) {}
        return null;
    }
}
