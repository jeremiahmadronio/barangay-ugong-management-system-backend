package com.barangay.cases.bcpc.service;

import com.barangay.cases.bcpc.dto.BcpcCarCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCarCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCarCaseUpdateRequest;
import com.barangay.cases.bcpc.model.BcpcCarAssessment;
import com.barangay.cases.bcpc.model.BcpcCaseDetails;
import com.barangay.cases.bcpc.repository.BcpcCarAssessmentRepository;
import com.barangay.cases.bcpc.repository.BcpcCaseDetailsRepository;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.CaseEvidence;
import com.barangay.cases.shared.model.IncidentDetail;
import com.barangay.cases.shared.model.NarrativeStatement;
import com.barangay.cases.shared.repository.CaseEvidenceRepository;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.witness.model.Witness;
import com.barangay.people.witness.repository.WitnessRepository;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.dto.EvidenceCreateDto;
import com.barangay.zshared.dto.EvidenceDetailDto;
import com.barangay.zshared.dto.IncidentDetailDto;
import com.barangay.zshared.dto.WitnessCreateDto;
import com.barangay.zshared.dto.WitnessDetailDto;
import com.barangay.zshared.enumerated.BcpcCategory;
import com.barangay.zshared.enumerated.CarOccurrenceCategory;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import com.barangay.zshared.enumerated.TimelineEventType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.mapper.CaseEvidenceMapper;
import com.barangay.zshared.mapper.ComplainantMapper;
import com.barangay.zshared.mapper.IncidentDetailMapper;
import com.barangay.zshared.mapper.NarrativeStatementMapper;
import com.barangay.zshared.mapper.PersonMapper;
import com.barangay.zshared.mapper.WitnessMapper;
import com.barangay.zshared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing Child at Risk (CAR) cases under BCPC.
 * Separated cleanly from CICL to enforce distinct statutory and administrative workflows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BcpcCarCaseService {

    private final CaseRepository caseRepository;
    private final BcpcCaseDetailsRepository bcpcCaseDetailsRepository;
    private final BcpcCarAssessmentRepository bcpcCarAssessmentRepository;
    private final PersonRepository personRepository;
    private final DepartmentRepository departmentRepository;
    private final OfficerRepository officerRepository;
    private final WitnessRepository witnessRepository;
    private final CaseEvidenceRepository caseEvidenceRepository;
    private final CaseTimelineService caseTimelineService;
    private final BcpcCommonService bcpcCommonService;

    private final PersonMapper personMapper;
    private final ComplainantMapper complainantMapper;
    private final IncidentDetailMapper incidentDetailMapper;
    private final NarrativeStatementMapper narrativeStatementMapper;
    private final CaseEvidenceMapper caseEvidenceMapper;
    private final WitnessMapper witnessMapper;

    /**
     * Files a new Child at Risk (CAR) case record under BCPC.
     */
    @Transactional
    public BcpcCarCaseDetailResponse createCarCase(BcpcCarCaseCreateRequest request) {
        log.info("Creating new BCPC Child at Risk (CAR) case...");

        Case aCase = new Case();
        aCase.setFilingType(FilingType.FORMAL_COMPLAINT);
        aCase.setCaseStatus(CaseStatus.PENDING);
        aCase.setIsArchived(false);

        String caseNumber = request.caseNumber();
        if (caseNumber == null || caseNumber.isBlank() || caseRepository.existsByCaseNumber(caseNumber.trim())) {
            caseNumber = bcpcCommonService.generateUniqueBcpcCaseNumber();
        }
        aCase.setCaseNumber(caseNumber);

        departmentRepository.findByName("BCPC").ifPresent(aCase::setDepartment);
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            aCase.setCaseFilledBy(currentUser);
        }

        if (request.assignedOfficerId() != null) {
            officerRepository.findById(request.assignedOfficerId()).ifPresent(aCase::setOfficer);
        } else if (request.assignedOfficer() != null && !request.assignedOfficer().isBlank()) {
            bcpcCommonService.resolveOfficerByName(request.assignedOfficer()).ifPresent(aCase::setOfficer);
        }

        Person reporterPerson = bcpcCommonService.resolveOrCreatePerson(request.reporter());
        if (reporterPerson != null) {
            Complainant complainant = new Complainant();
            complainant.setPerson(reporterPerson);
            complainant.setRelationshipToChild(request.reporterRelationshipToChild());
            aCase.setComplainant(complainant);
        }

        if (request.incident() != null) {
            IncidentDetail incidentDetail = incidentDetailMapper.fromCreateDto(request.incident());
            aCase.setIncidentDetail(incidentDetail);
        }

        if (request.narrative() != null) {
            NarrativeStatement narrativeStatement = narrativeStatementMapper.fromCreateDto(request.narrative());
            aCase.setNarrativeStatement(narrativeStatement);
        }

        String riskLevel = request.riskLevel() != null && !request.riskLevel().isBlank()
                ? request.riskLevel().toUpperCase()
                : (Boolean.TRUE.equals(request.hasSafetyWelfareConcerns()) ? "MODERATE" : "LOW");
        aCase.setRiskLevel(riskLevel);

        aCase.setIsAttested(Boolean.TRUE.equals(request.certifiedTrue()));
        if (Boolean.TRUE.equals(aCase.getIsAttested())) {
            aCase.setAttestedAt(LocalDateTime.now());
        }

        Case savedCase = caseRepository.save(aCase);

        if (request.witnesses() != null && !request.witnesses().isEmpty()) {
            List<Witness> witnessList = new ArrayList<>();
            for (WitnessCreateDto wDto : request.witnesses()) {
                Person wPerson = null;
                if (wDto.personId() != null) {
                    wPerson = personRepository.findById(wDto.personId()).orElse(null);
                }
                if (wPerson == null && wDto.firstName() != null && !wDto.firstName().isBlank()) {
                    wPerson = new Person();
                    wPerson.setFirstName(wDto.firstName().trim());
                    wPerson.setLastName(wDto.lastName() != null ? wDto.lastName().trim() : "");
                    wPerson.setContactNumber(wDto.contactNumber());
                    wPerson.setCompleteAddress(wDto.completeAddress());
                    wPerson.setIsResident(false);
                    wPerson = personRepository.save(wPerson);
                }
                if (wPerson != null) {
                    Witness witness = witnessMapper.toWitness(wPerson, savedCase, wDto);
                    witness = witnessRepository.save(witness);
                    witnessList.add(witness);
                }
            }
            savedCase.setWitnesses(witnessList);
        }

        if (request.evidences() != null && !request.evidences().isEmpty()) {
            for (EvidenceCreateDto eDto : request.evidences()) {
                CaseEvidence evidence = caseEvidenceMapper.fromCreateDto(eDto, savedCase);
                if (evidence != null) {
                    caseEvidenceRepository.save(evidence);
                }
            }
        }

        Person childPerson = bcpcCommonService.resolveOrCreatePerson(request.child());
        if (childPerson == null) {
            throw new IllegalArgumentException("Child information is required for BCPC cases.");
        }

        boolean isGuardianAvailable = request.isGuardianAvailable() != null ? request.isGuardianAvailable() : true;
        Person guardianPerson = isGuardianAvailable ? bcpcCommonService.resolveOrCreatePerson(request.guardian()) : null;

        BcpcCaseDetails bcpcDetails = new BcpcCaseDetails();
        bcpcDetails.setCases(savedCase);
        bcpcDetails.setCategory(BcpcCategory.CAR);
        bcpcDetails.setChild(childPerson);
        bcpcDetails.setGradeLevel(request.gradeLevel());
        bcpcDetails.setSchoolName(request.schoolName());
        bcpcDetails.setGuardian(guardianPerson);
        bcpcDetails.setGuardianRelationship(request.guardianRelationship());
        bcpcDetails.setComplainantRole(request.reporterRole());
        bcpcDetails.setIsGuardianPresentAtIntake(isGuardianAvailable);
        bcpcDetails.setGuardianUnavailableReason(request.guardianUnavailableReason());
        BcpcCaseDetails savedBcpcDetails = bcpcCaseDetailsRepository.save(bcpcDetails);

        BcpcCarAssessment carAssessment = new BcpcCarAssessment();
        carAssessment.setBcpcCaseDetails(savedBcpcDetails);
        carAssessment.setCarActCategory(request.carActCategory());
        carAssessment.setSpecificCarAct(request.specificCarAct());
        carAssessment.setSpecificCarActOther(request.specificCarActOther());
        carAssessment.setCarOccurrenceCategory(
                request.carOccurrenceCategory() != null ? request.carOccurrenceCategory() : CarOccurrenceCategory.FIRST_OCCURRENCE
        );
        carAssessment.setNeedsProtectiveCustody(Boolean.TRUE.equals(request.hasSafetyWelfareConcerns()));
        carAssessment.setSafetyAndWelfareNotes(request.safetyWelfareNotes());
        carAssessment.setRiskLevel(riskLevel);
        BcpcCarAssessment savedCarAssessment = bcpcCarAssessmentRepository.save(carAssessment);

        caseTimelineService.logEvent(
                savedCase,
                TimelineEventType.CASE_FILED,
                "BCPC Case Filed (CAR)",
                "Child at Risk Case #" + savedCase.getCaseNumber() + " was formally filed for child " + personMapper.buildFullName(childPerson) + ".",
                currentUser
        );

        log.info("BCPC CAR Case #{} created successfully.", savedCase.getCaseNumber());
        return mapToCarDetailResponse(savedCase, savedBcpcDetails, savedCarAssessment);
    }

    /**
     * Retrieves a single BCPC CAR case detail by Case ID.
     */
    @Transactional(readOnly = true)
    public BcpcCarCaseDetailResponse getCarCaseById(Long caseId) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + caseId));

        BcpcCaseDetails details = bcpcCaseDetailsRepository.findByCases(aCase)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC details not found for case ID: " + caseId));

        if (details.getCategory() != BcpcCategory.CAR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Case ID " + caseId + " is not classified as a CAR case.");
        }

        BcpcCarAssessment car = bcpcCarAssessmentRepository.findByBcpcCaseDetails(details)
                .orElse(null);

        return mapToCarDetailResponse(aCase, details, car);
    }

    /**
     * Updates an existing Child at Risk (CAR) case.
     * Preserves child and reporter identity while selectively updating incident specifics,
     * guardian status, CAR assessment, narrative, evidences, and witnesses.
     */
    @Transactional
    public BcpcCarCaseDetailResponse updateCarCase(Long caseId, BcpcCarCaseUpdateRequest request) {
        log.info("Updating BCPC Child at Risk (CAR) case ID: {}", caseId);

        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + caseId));

        if (aCase.getCaseStatus() == CaseStatus.SETTLED || aCase.getCaseStatus() == CaseStatus.DISMISSED || aCase.getCaseStatus() == CaseStatus.WITHDRAWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot edit settled, dismissed, or withdrawn cases.");
        }

        BcpcCaseDetails details = bcpcCaseDetailsRepository.findByCases(aCase)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC details not found for case ID: " + caseId));

        if (details.getCategory() != BcpcCategory.CAR) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Case ID " + caseId + " is not a CAR case.");
        }

        BcpcCarAssessment car = bcpcCarAssessmentRepository.findByBcpcCaseDetails(details)
                .orElseGet(() -> {
                    BcpcCarAssessment newCar = new BcpcCarAssessment();
                    newCar.setBcpcCaseDetails(details);
                    return bcpcCarAssessmentRepository.save(newCar);
                });

        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            aCase.setCaseUpdatedBy(currentUser);
        }

        // Update Reporter Role and Relationship
        if (request.reporterRelationshipToChild() != null && aCase.getComplainant() != null) {
            aCase.getComplainant().setRelationshipToChild(request.reporterRelationshipToChild());
        }
        if (request.reporterRole() != null) {
            details.setComplainantRole(request.reporterRole());
        }

        // Update Parent / Guardian & Education Status
        if (request.isGuardianAvailable() != null) {
            details.setIsGuardianPresentAtIntake(request.isGuardianAvailable());
            if (Boolean.TRUE.equals(request.isGuardianAvailable())) {
                if (request.guardian() != null) {
                    Person guardianPerson = bcpcCommonService.resolveOrCreatePerson(request.guardian());
                    details.setGuardian(guardianPerson);
                }
                if (request.guardianRelationship() != null) {
                    details.setGuardianRelationship(request.guardianRelationship());
                }
                details.setGuardianUnavailableReason(null);
            } else {
                if (request.guardianUnavailableReason() != null) {
                    details.setGuardianUnavailableReason(request.guardianUnavailableReason());
                }
            }
        } else {
            if (request.guardian() != null && details.getGuardian() != null) {
                bcpcCommonService.resolveOrCreatePerson(request.guardian());
            }
            if (request.guardianRelationship() != null) {
                details.setGuardianRelationship(request.guardianRelationship());
            }
            if (request.guardianUnavailableReason() != null) {
                details.setGuardianUnavailableReason(request.guardianUnavailableReason());
            }
        }

        if (request.gradeLevel() != null) details.setGradeLevel(request.gradeLevel());
        if (request.schoolName() != null) details.setSchoolName(request.schoolName());

        // Update Incident Details
        if (request.incident() != null) {
            if (aCase.getIncidentDetail() == null) {
                aCase.setIncidentDetail(incidentDetailMapper.fromCreateDto(request.incident()));
            } else {
                IncidentDetail inc = aCase.getIncidentDetail();
                if (request.incident().natureOfComplaint() != null) {
                    String nat = request.incident().natureOfComplaint().trim();
                    if (nat.toUpperCase().startsWith("CAR:")) nat = nat.substring(4).trim();
                    if (nat.toUpperCase().startsWith("CICL:")) nat = nat.substring(5).trim();
                    inc.setNatureOfComplaint(nat);
                }
                if (request.incident().placeOfIncident() != null) inc.setIncidentLocation(request.incident().placeOfIncident());
                if (request.incident().descriptionOfInjuries() != null) inc.setInjuriesDamagesDescription(request.incident().descriptionOfInjuries());
                if (request.incident().dateOfIncident() != null && !request.incident().dateOfIncident().isBlank()) {
                    try {
                        inc.setDateOfIncident(java.time.LocalDate.parse(request.incident().dateOfIncident()));
                    } catch (Exception ignored) {}
                }
                if (request.incident().timeOfIncident() != null && !request.incident().timeOfIncident().isBlank()) {
                    try {
                        String t = request.incident().timeOfIncident();
                        inc.setTimeOfIncident(java.time.LocalTime.parse(t.length() == 5 ? t + ":00" : t));
                    } catch (Exception ignored) {}
                }
            }
        }

        // Update CAR Assessment Specifics
        if (request.carActCategory() != null) car.setCarActCategory(request.carActCategory());
        if (request.specificCarAct() != null) car.setSpecificCarAct(request.specificCarAct());
        if (request.specificCarActOther() != null) car.setSpecificCarActOther(request.specificCarActOther());
        if (request.carOccurrenceCategory() != null) car.setCarOccurrenceCategory(request.carOccurrenceCategory());
        if (request.hasAdditionalProtectionConcern() != null) car.setNeedsProtectiveCustody(request.hasAdditionalProtectionConcern());
        if (request.safetyNotes() != null) car.setSafetyAndWelfareNotes(request.safetyNotes());

        // Update Narrative Statement
        if (request.narrative() != null) {
            bcpcCommonService.updateNarrative(aCase, request.narrative());
        }

        // Update Supporting Evidences
        if (request.evidences() != null) {
            bcpcCommonService.updateEvidences(aCase, request.evidences());
        }

        // Update Witnesses
        if (request.witnesses() != null) {
            bcpcCommonService.updateWitnesses(aCase, request.witnesses());
        }

        bcpcCaseDetailsRepository.save(details);
        bcpcCarAssessmentRepository.save(car);
        Case savedCase = caseRepository.save(aCase);

        try {
            caseTimelineService.logEvent(
                    savedCase,
                    TimelineEventType.STATUS_CHANGED,
                    "CAR Case Details Updated",
                    "Child at Risk Case #" + savedCase.getCaseNumber() + " details were updated.",
                    currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to log timeline event for CAR update: {}", e.getMessage());
        }

        log.info("BCPC CAR Case #{} updated successfully.", savedCase.getCaseNumber());
        return mapToCarDetailResponse(savedCase, details, car);
    }

    public BcpcCarCaseDetailResponse mapToCarDetailResponse(Case aCase, BcpcCaseDetails details, BcpcCarAssessment car) {
        var reporterDto = aCase.getComplainant() != null ? complainantMapper.toDetailDto(aCase.getComplainant()) : null;
        String reporterRel = aCase.getComplainant() != null ? aCase.getComplainant().getRelationshipToChild() : null;
        var incidentDto = aCase.getIncidentDetail() != null ? incidentDetailMapper.toDetailDto(aCase.getIncidentDetail()) : null;
        if (incidentDto != null && incidentDto.natureOfComplaint() != null) {
            String cleanNat = incidentDto.natureOfComplaint().trim();
            if (cleanNat.toUpperCase().startsWith("CAR:")) cleanNat = cleanNat.substring(4).trim();
            if (cleanNat.toUpperCase().startsWith("CICL:")) cleanNat = cleanNat.substring(5).trim();
            incidentDto = new IncidentDetailDto(
                    incidentDto.id(),
                    cleanNat,
                    incidentDto.dateOfIncident(),
                    incidentDto.timeOfIncident(),
                    incidentDto.placeOfIncident(),
                    incidentDto.frequencyOfIncident(),
                    incidentDto.descriptionOfInjuries(),
                    incidentDto.relatedBarangayService()
            );
        }
        var narrativeDto = aCase.getNarrativeStatement() != null ? narrativeStatementMapper.toDetailDto(aCase.getNarrativeStatement()) : null;

        List<EvidenceDetailDto> evidenceDtos = caseEvidenceRepository.findByCasesId(aCase.getId()).stream()
                .map(caseEvidenceMapper::toDetailDto)
                .toList();

        List<WitnessDetailDto> witnessDtos = witnessRepository.findByCaseId(aCase.getId()).stream()
                .map(witnessMapper::toDetailDto)
                .toList();

        String reportingOfficer = aCase.getCaseFilledBy() != null && aCase.getCaseFilledBy().getPerson() != null
                ? personMapper.buildFullName(aCase.getCaseFilledBy().getPerson())
                : (aCase.getCaseFilledBy() != null ? aCase.getCaseFilledBy().getUsername() : "BCPC Officer");

        String assignedOfficer = aCase.getOfficer() != null && aCase.getOfficer().getPerson() != null
                ? personMapper.buildFullName(aCase.getOfficer().getPerson())
                : null;

        String caseStatus = aCase.getCaseStatus() != null ? aCase.getCaseStatus().name() : "PENDING";

        return new BcpcCarCaseDetailResponse(
                aCase.getId(),
                aCase.getCaseNumber(),
                aCase.getFilingType() != null ? aCase.getFilingType().name() : "FORMAL_COMPLAINT",
                caseStatus,
                aCase.getCaseStatusRemarks(),
                aCase.getRiskLevel(),
                aCase.getCreatedAt(),
                reportingOfficer,
                assignedOfficer,

                reporterDto,
                details != null ? details.getComplainantRole() : null,
                reporterRel,

                bcpcCommonService.toPersonDetailDto(details != null ? details.getChild() : null),
                details != null ? details.getGradeLevel() : null,
                details != null ? details.getSchoolName() : null,

                bcpcCommonService.toPersonDetailDto(details != null ? details.getGuardian() : null),
                details != null ? details.getGuardianRelationship() : null,
                details != null ? details.getIsGuardianPresentAtIntake() : true,
                details != null ? details.getGuardianUnavailableReason() : null,

                incidentDto,
                car != null ? car.getCarActCategory() : null,
                car != null ? car.getSpecificCarAct() : null,
                car != null ? car.getSpecificCarActOther() : null,
                car != null ? car.getCarOccurrenceCategory() : null,
                car != null ? car.getNeedsProtectiveCustody() : false,
                car != null ? car.getTemporaryShelterPlacement() : null,
                car != null ? car.getSafetyAndWelfareNotes() : null,

                narrativeDto,
                evidenceDtos,
                witnessDtos,
                aCase.getIsAttested(),
                aCase.getAttestedAt()
        );
    }
}
