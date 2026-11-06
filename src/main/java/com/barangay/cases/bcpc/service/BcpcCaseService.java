package com.barangay.cases.bcpc.service;

import com.barangay.cases.bcpc.dto.BcpcCarCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCarCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCiclAssessmentDto;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcPersonDetailDto;
import com.barangay.cases.bcpc.dto.BcpcPersonInputDto;
import com.barangay.cases.bcpc.model.BcpcCarAssessment;
import com.barangay.cases.bcpc.model.BcpcCaseDetails;
import com.barangay.cases.bcpc.model.BcpcCiclAssessment;
import com.barangay.cases.bcpc.repository.BcpcCarAssessmentRepository;
import com.barangay.cases.bcpc.repository.BcpcCaseDetailsRepository;
import com.barangay.cases.bcpc.repository.BcpcCiclAssessmentRepository;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.CaseEvidence;
import com.barangay.cases.shared.model.IncidentDetail;
import com.barangay.cases.shared.model.NarrativeStatement;
import com.barangay.cases.shared.repository.CaseEvidenceRepository;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.repository.EvidenceTypeRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.people.witness.model.Witness;
import com.barangay.people.witness.repository.WitnessRepository;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.dto.EvidenceCreateDto;
import com.barangay.zshared.dto.EvidenceDetailDto;
import com.barangay.zshared.dto.WitnessCreateDto;
import com.barangay.zshared.dto.WitnessDetailDto;
import com.barangay.zshared.enumerated.BcpcCategory;
import com.barangay.zshared.enumerated.CarOccurrenceCategory;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.CiclPathway;
import com.barangay.zshared.enumerated.DiscernmentStatus;
import com.barangay.zshared.enumerated.DiversionAuthority;
import com.barangay.zshared.enumerated.FilingType;
import com.barangay.zshared.enumerated.PenaltyRoutingCategory;
import com.barangay.zshared.enumerated.TimelineEventType;
import com.barangay.zshared.mapper.CaseEvidenceMapper;
import com.barangay.zshared.mapper.ComplainantMapper;
import com.barangay.zshared.mapper.IncidentDetailMapper;
import com.barangay.zshared.mapper.NarrativeStatementMapper;
import com.barangay.zshared.mapper.PersonMapper;
import com.barangay.zshared.mapper.WitnessMapper;
import com.barangay.zshared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service managing cases under the Barangay Council for the Protection of Children (BCPC).
 * Implements normalized and legally aligned workflows for:
 * 1. Child at Risk (CAR) - vulnerability, juvenile status offenses, protection concerns
 * 2. Child in Conflict with the Law (CICL) - alleged offenses under RA 9344 / RA 10630, age exemption, discernment, and diversion routing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BcpcCaseService {

    private final CaseRepository caseRepository;
    private final BcpcCaseDetailsRepository bcpcCaseDetailsRepository;
    private final BcpcCarAssessmentRepository bcpcCarAssessmentRepository;
    private final BcpcCiclAssessmentRepository bcpcCiclAssessmentRepository;
    private final PersonRepository personRepository;
    private final ResidentRepository residentRepository;
    private final WitnessRepository witnessRepository;
    private final EvidenceTypeRepository evidenceTypeRepository;
    private final CaseEvidenceRepository caseEvidenceRepository;
    private final DepartmentRepository departmentRepository;
    private final OfficerRepository officerRepository;
    private final CaseTimelineService caseTimelineService;

    // Shared Mappers
    private final PersonMapper personMapper;
    private final ComplainantMapper complainantMapper;
    private final IncidentDetailMapper incidentDetailMapper;
    private final NarrativeStatementMapper narrativeStatementMapper;
    private final WitnessMapper witnessMapper;
    private final CaseEvidenceMapper caseEvidenceMapper;

    private static final SecureRandom RANDOM = new SecureRandom();

    // =========================================================================
    // CAR (Child at Risk) Case Filing & Retrieval
    // =========================================================================

    /**
     * Files a new Child at Risk (CAR) case.
     * Maps the reporter as complainant, child as subject CAR minor, and records safety/welfare assessments.
     *
     * @param request the CAR intake payload
     * @return the created case detail response
     */
    @Transactional
    public BcpcCarCaseDetailResponse createCarCase(BcpcCarCaseCreateRequest request) {
        log.info("Creating new BCPC Child at Risk (CAR) case...");

        Case aCase = new Case();

        // 1. Filing Type & Status
        aCase.setFilingType(FilingType.FORMAL_COMPLAINT);
        aCase.setCaseStatus(CaseStatus.PENDING);
        aCase.setIsArchived(false);

        // 2. Case Number
        String caseNumber = request.caseNumber();
        if (caseNumber == null || caseNumber.isBlank() || caseRepository.existsByCaseNumber(caseNumber.trim())) {
            caseNumber = generateUniqueBcpcCaseNumber();
        }
        aCase.setCaseNumber(caseNumber);

        // 3. Department & Reporting User
        departmentRepository.findByName("BCPC").ifPresent(aCase::setDepartment);
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            aCase.setCaseFilledBy(currentUser);
        }

        // 4. Assigned Officer
        if (request.assignedOfficerId() != null) {
            officerRepository.findById(request.assignedOfficerId()).ifPresent(aCase::setOfficer);
        } else if (request.assignedOfficer() != null && !request.assignedOfficer().isBlank()) {
            resolveOfficerByName(request.assignedOfficer()).ifPresent(aCase::setOfficer);
        }

        // 5. C1: Reporter / Complainant (The person who reported the Child at Risk)
        Person reporterPerson = resolveOrCreatePerson(request.reporter());
        if (reporterPerson != null) {
            Complainant complainant = new Complainant();
            complainant.setPerson(reporterPerson);
            complainant.setRelationshipToChild(request.reporterRelationshipToChild());
            aCase.setComplainant(complainant);
        }

        // 6. E: Incident Details
        if (request.incident() != null) {
            IncidentDetail incidentDetail = incidentDetailMapper.fromCreateDto(request.incident());
            aCase.setIncidentDetail(incidentDetail);
        }

        // 7. F: Narrative Statement
        if (request.narrative() != null) {
            NarrativeStatement narrativeStatement = narrativeStatementMapper.fromCreateDto(request.narrative());
            aCase.setNarrativeStatement(narrativeStatement);
        }

        // 8. Risk Level & Attestation
        String riskLevel = request.riskLevel() != null && !request.riskLevel().isBlank()
                ? request.riskLevel().toUpperCase()
                : (Boolean.TRUE.equals(request.hasSafetyWelfareConcerns()) ? "MODERATE" : "LOW");
        aCase.setRiskLevel(riskLevel);

        aCase.setIsAttested(Boolean.TRUE.equals(request.certifiedTrue()));
        if (Boolean.TRUE.equals(aCase.getIsAttested())) {
            aCase.setAttestedAt(LocalDateTime.now());
        }

        // 9. Initial Save of Case
        Case savedCase = caseRepository.save(aCase);

        // 10. H: Witnesses
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

        // 11. G: Supporting Evidences
        if (request.evidences() != null && !request.evidences().isEmpty()) {
            for (EvidenceCreateDto eDto : request.evidences()) {
                CaseEvidence evidence = caseEvidenceMapper.fromCreateDto(eDto, savedCase);
                if (evidence != null) {
                    caseEvidenceRepository.save(evidence);
                }
            }
        }

        // 12. C2 & D: BCPC Intake Profile (Child & Guardian)
        Person childPerson = resolveOrCreatePerson(request.child());
        if (childPerson == null) {
            throw new IllegalArgumentException("Child information is required for BCPC cases.");
        }

        boolean isGuardianAvailable = request.isGuardianAvailable() != null ? request.isGuardianAvailable() : true;
        Person guardianPerson = isGuardianAvailable ? resolveOrCreatePerson(request.guardian()) : null;

        BcpcCaseDetails bcpcDetails = new BcpcCaseDetails();
        bcpcDetails.setCases(savedCase);
        bcpcDetails.setCategory(BcpcCategory.CAR);
        bcpcDetails.setChild(childPerson);
        bcpcDetails.setGradeLevel(request.gradeLevel());
        bcpcDetails.setSchoolName(request.schoolName());
        bcpcDetails.setGuardian(guardianPerson);
        bcpcDetails.setGuardianRelationship(request.guardianRelationship());
        bcpcDetails.setIsGuardianPresentAtIntake(isGuardianAvailable);
        bcpcDetails.setGuardianUnavailableReason(request.guardianUnavailableReason());
        BcpcCaseDetails savedBcpcDetails = bcpcCaseDetailsRepository.save(bcpcDetails);

        // 13. E: CAR Specific Assessment & Welfare
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

        // 14. Case Timeline Event Logging
        caseTimelineService.logEvent(
                savedCase,
                TimelineEventType.CASE_FILED,
                "BCPC Case Filed (CAR)",
                "Child at Risk Case #" + savedCase.getCaseNumber() + " was formally filed for child " + personMapper.buildFullName(childPerson) + " with Risk Level: " + savedCase.getRiskLevel(),
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
                .orElseThrow(() -> new IllegalArgumentException("Case not found with ID: " + caseId));

        BcpcCaseDetails details = bcpcCaseDetailsRepository.findByCases(aCase)
                .orElseThrow(() -> new IllegalArgumentException("BCPC details not found for case ID: " + caseId));

        if (details.getCategory() != BcpcCategory.CAR) {
            throw new IllegalArgumentException("Case ID " + caseId + " is not classified as a CAR case.");
        }

        BcpcCarAssessment car = bcpcCarAssessmentRepository.findByBcpcCaseDetails(details)
                .orElse(null);

        return mapToCarDetailResponse(aCase, details, car);
    }

    // =========================================================================
    // CICL (Child in Conflict with the Law) Case Filing & Retrieval
    // =========================================================================

    /**
     * Files a new Child in Conflict with the Law (CICL) case.
     * Enforces Philippine Juvenile Justice and Welfare Act (RA 9344 / RA 10630):
     * - Child is the Respondent minor alleged to have committed an offense.
     * - Victim is the Complainant (or tanod/officer if victimless offense).
     * - Enforces age exemption (<= 15 years old exempt from criminal liability -> Intervention only).
     * - Enforces discernment and diversion routing.
     *
     * @param request the CICL intake payload
     * @return the created CICL case detail response
     */
    @Transactional
    public BcpcCiclCaseDetailResponse createCiclCase(BcpcCiclCaseCreateRequest request) {
        log.info("Creating new BCPC Child in Conflict with the Law (CICL) case...");

        // 1. Resolve Child Person (Mandatory)
        Person childPerson = resolveOrCreatePerson(request.child());
        if (childPerson == null) {
            throw new IllegalArgumentException("Child information is required for BCPC cases.");
        }

        // 2. Validate Minor Status (Must be below 18 years old)
        LocalDate incidentDate = null;
        if (request.incident() != null && request.incident().dateOfIncident() != null && !request.incident().dateOfIncident().isBlank()) {
            try {
                incidentDate = LocalDate.parse(request.incident().dateOfIncident().trim());
            } catch (Exception ignored) {}
        }

        int calculatedAge = 0;
        if (childPerson.getBirthDate() != null && incidentDate != null) {
            calculatedAge = Period.between(childPerson.getBirthDate(), incidentDate).getYears();
        } else if (childPerson.getAge() != null) {
            calculatedAge = childPerson.getAge();
        } else if (request.ageAtIncident() != null) {
            calculatedAge = request.ageAtIncident();
        }

        if (calculatedAge >= 18) {
            throw new IllegalArgumentException("The individual was " + calculatedAge + " years old at the time of the incident and cannot be processed as a Child in Conflict with the Law (CICL). Adult criminal proceedings apply.");
        }

        // 3. Resolve Victim / Complainant
        Person victimPerson = null;
        if (!Boolean.TRUE.equals(request.isVictimlessOffense()) && request.victim() != null) {
            victimPerson = resolveOrCreatePerson(request.victim());
            if (victimPerson != null && childPerson.getId() != null && victimPerson.getId() != null) {
                if (victimPerson.getId().equals(childPerson.getId())) {
                    throw new IllegalArgumentException("The Victim/Offended Party and CICL Child cannot be the same resident in this case.");
                }
            }
        }

        // 4. Determine Legal Pathway & Routing under RA 9344 / RA 10630
        CiclPathway determinedPathway = request.ciclPathway() != null ? request.ciclPathway() : CiclPathway.PENDING;
        DiscernmentStatus discernment = request.discernmentStatus() != null ? request.discernmentStatus() : DiscernmentStatus.PENDING_LSWDO_EVALUATION;
        PenaltyRoutingCategory penaltyRouting = request.penaltyRoutingCategory() != null ? request.penaltyRoutingCategory() : PenaltyRoutingCategory.UNDETERMINED;
        DiversionAuthority authority = request.diversionAuthority() != null ? request.diversionAuthority() : DiversionAuthority.UNDETERMINED;
        boolean isDiversionEligible = Boolean.TRUE.equals(request.isDiversionEligible());

        if (calculatedAge > 0 && calculatedAge <= 15) {
            // Under RA 9344 Sec. 20 (as amended by RA 10630), child 15 or below is exempt from criminal liability -> INTERVENTION
            determinedPathway = CiclPathway.INTERVENTION;
            isDiversionEligible = false;
            discernment = DiscernmentStatus.WITHOUT_DISCERNMENT;
        } else if (calculatedAge > 15 && calculatedAge < 18) {
            if (discernment == DiscernmentStatus.WITHOUT_DISCERNMENT) {
                determinedPathway = CiclPathway.INTERVENTION;
                isDiversionEligible = false;
            } else if (discernment == DiscernmentStatus.WITH_DISCERNMENT) {
                if (penaltyRouting == PenaltyRoutingCategory.SIX_YEARS_OR_LESS) {
                    determinedPathway = CiclPathway.DIVERSION;
                    isDiversionEligible = true;
                    if (authority == DiversionAuthority.UNDETERMINED) {
                        authority = DiversionAuthority.BARANGAY;
                    }
                } else if (penaltyRouting == PenaltyRoutingCategory.ABOVE_SIX_YEARS) {
                    determinedPathway = CiclPathway.DIVERSION;
                    isDiversionEligible = false;
                    if (authority == DiversionAuthority.UNDETERMINED) {
                        authority = DiversionAuthority.COURT;
                    }
                }
            }
        }

        // 5. Initialize Universal Case Entity
        Case aCase = new Case();
        aCase.setFilingType(FilingType.FORMAL_COMPLAINT);
        aCase.setCaseStatus(CaseStatus.PENDING);
        aCase.setIsArchived(false);

        // Case Number
        String caseNumber = request.caseNumber();
        if (caseNumber == null || caseNumber.isBlank() || caseRepository.existsByCaseNumber(caseNumber.trim())) {
            caseNumber = generateUniqueBcpcCaseNumber();
        }
        aCase.setCaseNumber(caseNumber);

        // Department & Reporting User
        departmentRepository.findByName("BCPC").ifPresent(aCase::setDepartment);
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            aCase.setCaseFilledBy(currentUser);
        }

        // Assigned Officer
        if (request.assignedOfficerId() != null) {
            officerRepository.findById(request.assignedOfficerId()).ifPresent(aCase::setOfficer);
        } else if (request.assignedOfficer() != null && !request.assignedOfficer().isBlank()) {
            resolveOfficerByName(request.assignedOfficer()).ifPresent(aCase::setOfficer);
        }

        // 6. Set Parties (Complainant = Victim, Respondent = CICL Child)
        if (victimPerson != null) {
            Complainant complainant = new Complainant();
            complainant.setPerson(victimPerson);
            complainant.setRelationshipToChild(request.victimRelationshipToChild());
            aCase.setComplainant(complainant);
        }

        Respondent respondent = new Respondent();
        respondent.setPerson(childPerson);
        if (request.victimRelationshipToChild() != null) {
            respondent.setRelationshipToComplainant(request.victimRelationshipToChild());
        }
        aCase.setRespondent(respondent);

        // 7. Incident Details & Alleged Violation
        if (request.incident() != null) {
            IncidentDetail incidentDetail = incidentDetailMapper.fromCreateDto(request.incident());
            if (incidentDetail.getNatureOfComplaint() == null || incidentDetail.getNatureOfComplaint().isBlank()) {
                incidentDetail.setNatureOfComplaint(request.allegedViolation());
            }
            aCase.setIncidentDetail(incidentDetail);
        }

        // 8. Narrative Statement
        if (request.narrative() != null) {
            NarrativeStatement narrativeStatement = narrativeStatementMapper.fromCreateDto(request.narrative());
            aCase.setNarrativeStatement(narrativeStatement);
        }

        // 9. Risk Level & Attestation
        String riskLevel = Boolean.TRUE.equals(request.isRepeatOffender()) ? "HIGH" : "MODERATE";
        aCase.setRiskLevel(riskLevel);

        aCase.setIsAttested(Boolean.TRUE.equals(request.certifiedTrue()));
        if (Boolean.TRUE.equals(aCase.getIsAttested())) {
            aCase.setAttestedAt(LocalDateTime.now());
        }

        // 10. Initial Save of Case
        Case savedCase = caseRepository.save(aCase);

        // 11. Witnesses
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

        // 12. Supporting Evidences
        if (request.evidences() != null && !request.evidences().isEmpty()) {
            for (EvidenceCreateDto eDto : request.evidences()) {
                CaseEvidence evidence = caseEvidenceMapper.fromCreateDto(eDto, savedCase);
                if (evidence != null) {
                    caseEvidenceRepository.save(evidence);
                }
            }
        }

        // 13. Parent / Guardian Details
        boolean isGuardianAvailable = request.isGuardianAvailable() != null ? request.isGuardianAvailable() : true;
        Person guardianPerson = isGuardianAvailable ? resolveOrCreatePerson(request.guardian()) : null;

        BcpcCaseDetails bcpcDetails = new BcpcCaseDetails();
        bcpcDetails.setCases(savedCase);
        bcpcDetails.setCategory(BcpcCategory.CICL);
        bcpcDetails.setChild(childPerson);
        bcpcDetails.setGradeLevel(request.gradeLevel());
        bcpcDetails.setSchoolName(request.schoolName());
        bcpcDetails.setGuardian(guardianPerson);
        bcpcDetails.setGuardianRelationship(request.guardianRelationship());
        bcpcDetails.setIsGuardianPresentAtIntake(isGuardianAvailable);
        bcpcDetails.setGuardianUnavailableReason(request.guardianUnavailableReason());
        BcpcCaseDetails savedBcpcDetails = bcpcCaseDetailsRepository.save(bcpcDetails);

        // 14. CICL Legal Assessment & Routing
        BcpcCiclAssessment ciclAssessment = new BcpcCiclAssessment();
        ciclAssessment.setBcpcCaseDetails(savedBcpcDetails);
        ciclAssessment.setAgeAtIncident(calculatedAge);
        ciclAssessment.setCiclPathway(determinedPathway);
        ciclAssessment.setDiscernmentStatus(discernment);
        ciclAssessment.setPenaltyRoutingCategory(penaltyRouting);
        ciclAssessment.setDiversionAuthority(authority);
        ciclAssessment.setIsDiversionEligible(isDiversionEligible);
        ciclAssessment.setAdmittedOffense(Boolean.TRUE.equals(request.admittedOffense()));
        ciclAssessment.setIsRepeatOffender(Boolean.TRUE.equals(request.isRepeatOffender()));
        ciclAssessment.setAllegedViolation(request.allegedViolation());
        ciclAssessment.setApprehendingOfficerName(request.apprehendingOfficerName());
        ciclAssessment.setApprehendingOfficerOffice(request.apprehendingOfficerOffice());
        ciclAssessment.setApprehendingOfficerContact(request.apprehendingOfficerContact());
        ciclAssessment.setApprehensionDateTime(request.apprehensionDateTime());
        ciclAssessment.setApprehensionPlace(request.apprehensionPlace());
        BcpcCiclAssessment savedCiclAssessment = bcpcCiclAssessmentRepository.save(ciclAssessment);

        // 15. Case Timeline Event Logging
        caseTimelineService.logEvent(
                savedCase,
                TimelineEventType.CASE_FILED,
                "BCPC Case Filed (CICL)",
                "Child in Conflict with the Law Case #" + savedCase.getCaseNumber() + " was formally filed for minor " + personMapper.buildFullName(childPerson) + ". Alleged violation: " + request.allegedViolation() + ". Legal pathway: " + determinedPathway,
                currentUser
        );

        log.info("BCPC CICL Case #{} created successfully.", savedCase.getCaseNumber());
        return mapToCiclDetailResponse(savedCase, savedBcpcDetails, savedCiclAssessment);
    }

    /**
     * Retrieves a single BCPC CICL case detail by Case ID.
     */
    @Transactional(readOnly = true)
    public BcpcCiclCaseDetailResponse getCiclCaseById(Long caseId) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found with ID: " + caseId));

        BcpcCaseDetails details = bcpcCaseDetailsRepository.findByCases(aCase)
                .orElseThrow(() -> new IllegalArgumentException("BCPC details not found for case ID: " + caseId));

        if (details.getCategory() != BcpcCategory.CICL) {
            throw new IllegalArgumentException("Case ID " + caseId + " is not classified as a CICL case.");
        }

        BcpcCiclAssessment cicl = bcpcCiclAssessmentRepository.findByBcpcCaseDetails(details)
                .orElse(null);

        return mapToCiclDetailResponse(aCase, details, cicl);
    }

    // =========================================================================
    // Person Resolution & Shared Utilities
    // =========================================================================

    /**
     * Resolves a Person by residentId, personId, or creates a new Person entity if manual.
     */
    public Person resolveOrCreatePerson(BcpcPersonInputDto dto) {
        if (dto == null) return null;

        if (dto.residentId() != null) {
            Optional<Resident> residentOpt = residentRepository.findById(dto.residentId());
            if (residentOpt.isPresent() && residentOpt.get().getPerson() != null) {
                return residentOpt.get().getPerson();
            }
        }

        if (dto.personId() != null) {
            Optional<Person> personOpt = personRepository.findById(dto.personId());
            if (personOpt.isPresent()) {
                return personOpt.get();
            }
        }

        // Manual Input: create new Person
        Person person = new Person();
        person.setFirstName(dto.firstName() != null ? dto.firstName().trim() : null);
        person.setLastName(dto.lastName() != null ? dto.lastName().trim() : null);
        person.setMiddleName(dto.middleName() != null && !dto.middleName().isBlank() ? dto.middleName().trim() : null);
        person.setSuffix(dto.suffix() != null && !dto.suffix().isBlank() ? dto.suffix().trim() : null);
        person.setContactNumber(dto.contactNumber());
        person.setCompleteAddress(dto.completeAddress());
        person.setAge(dto.age());
        person.setGender(dto.gender());
        person.setBirthDate(dto.birthDate());
        person.setCivilStatus(dto.civilStatus());
        person.setEmail(dto.email());
        person.setIsResident(false);

        return personRepository.save(person);
    }

    private BcpcPersonDetailDto toPersonDetailDto(Person p) {
        if (p == null) return null;
        return new BcpcPersonDetailDto(
                p.getId(),
                p.getFirstName(),
                p.getMiddleName(),
                p.getLastName(),
                personMapper.buildFullName(p),
                p.getSuffix(),
                p.getContactNumber(),
                p.getCompleteAddress(),
                p.getAge(),
                p.getGender(),
                p.getBirthDate(),
                p.getCivilStatus(),
                p.getEmail(),
                p.getIsResident()
        );
    }

    private BcpcCarCaseDetailResponse mapToCarDetailResponse(Case aCase, BcpcCaseDetails details, BcpcCarAssessment car) {
        var reporterDto = aCase.getComplainant() != null ? complainantMapper.toDetailDto(aCase.getComplainant()) : null;
        String reporterRel = aCase.getComplainant() != null ? aCase.getComplainant().getRelationshipToChild() : null;
        var incidentDto = aCase.getIncidentDetail() != null ? incidentDetailMapper.toDetailDto(aCase.getIncidentDetail()) : null;
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

        return new BcpcCarCaseDetailResponse(
                aCase.getId(),
                aCase.getCaseNumber(),
                aCase.getFilingType() != null ? aCase.getFilingType().name() : "FORMAL_COMPLAINT",
                aCase.getCaseStatus() != null ? aCase.getCaseStatus().name() : "PENDING",
                aCase.getCaseStatusRemarks(),
                aCase.getRiskLevel(),
                aCase.getCreatedAt(),
                reportingOfficer,
                assignedOfficer,

                reporterDto,
                null,
                reporterRel,

                toPersonDetailDto(details != null ? details.getChild() : null),
                details != null ? details.getGradeLevel() : null,
                details != null ? details.getSchoolName() : null,

                toPersonDetailDto(details != null ? details.getGuardian() : null),
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

    private BcpcCiclCaseDetailResponse mapToCiclDetailResponse(Case aCase, BcpcCaseDetails details, BcpcCiclAssessment cicl) {
        var victimDto = aCase.getComplainant() != null ? complainantMapper.toDetailDto(aCase.getComplainant()) : null;
        String victimRel = aCase.getComplainant() != null ? aCase.getComplainant().getRelationshipToChild() : null;
        var incidentDto = aCase.getIncidentDetail() != null ? incidentDetailMapper.toDetailDto(aCase.getIncidentDetail()) : null;
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

        BcpcCiclAssessmentDto ciclDto = cicl != null ? new BcpcCiclAssessmentDto(
                cicl.getAgeAtIncident(),
                cicl.getCiclPathway(),
                cicl.getDiscernmentStatus(),
                cicl.getPenaltyRoutingCategory(),
                cicl.getDiversionAuthority(),
                cicl.getIsDiversionEligible(),
                cicl.getAdmittedOffense(),
                cicl.getIsRepeatOffender(),
                cicl.getAllegedViolation(),
                cicl.getApprehendingOfficerName(),
                cicl.getApprehendingOfficerOffice(),
                cicl.getApprehendingOfficerContact(),
                cicl.getApprehensionDateTime(),
                cicl.getApprehensionPlace()
        ) : null;

        return new BcpcCiclCaseDetailResponse(
                aCase.getId(),
                aCase.getCaseNumber(),
                aCase.getFilingType() != null ? aCase.getFilingType().name() : "FORMAL_COMPLAINT",
                aCase.getCaseStatus() != null ? aCase.getCaseStatus().name() : "PENDING",
                aCase.getCaseStatusRemarks(),
                aCase.getRiskLevel(),
                aCase.getCreatedAt(),
                reportingOfficer,
                assignedOfficer,

                victimDto,
                aCase.getComplainant() == null,
                null,
                victimRel,

                toPersonDetailDto(details != null ? details.getChild() : null),
                details != null ? details.getGradeLevel() : null,
                details != null ? details.getSchoolName() : null,

                toPersonDetailDto(details != null ? details.getGuardian() : null),
                details != null ? details.getGuardianRelationship() : null,
                details != null ? details.getIsGuardianPresentAtIntake() : true,
                details != null ? details.getGuardianUnavailableReason() : null,

                ciclDto,
                incidentDto,
                narrativeDto,
                evidenceDtos,
                witnessDtos,
                aCase.getIsAttested(),
                aCase.getAttestedAt()
        );
    }

    private Optional<Officer> resolveOfficerByName(String officerName) {
        String clean = officerName.trim().toLowerCase();
        return officerRepository.findAll().stream()
                .filter(o -> o.getPerson() != null && (
                        personMapper.buildFullName(o.getPerson()).toLowerCase().contains(clean) ||
                                clean.contains(o.getPerson().getLastName().toLowerCase())
                ))
                .findFirst();
    }

    private String generateUniqueBcpcCaseNumber() {
        int fullYear = LocalDate.now().getYear();
        int twoDigitYear = fullYear % 100;
        String caseNumber;
        do {
            int randomNum = RANDOM.nextInt(9000) + 1;
            caseNumber = String.format("BU%02d-BCPC-%04d", twoDigitYear, randomNum);
        } while (caseRepository.existsByCaseNumber(caseNumber));
        return caseNumber;
    }
}
