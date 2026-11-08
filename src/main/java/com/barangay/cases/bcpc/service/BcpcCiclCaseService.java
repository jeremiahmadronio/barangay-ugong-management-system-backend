package com.barangay.cases.bcpc.service;

import com.barangay.cases.bcpc.dto.BcpcCiclAssessmentDto;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseUpdateRequest;
import com.barangay.cases.bcpc.model.BcpcCaseDetails;
import com.barangay.cases.bcpc.model.BcpcCiclAssessment;
import com.barangay.cases.bcpc.repository.BcpcCaseDetailsRepository;
import com.barangay.cases.bcpc.repository.BcpcCiclAssessmentRepository;
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
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.CiclPathway;
import com.barangay.zshared.enumerated.DiscernmentStatus;
import com.barangay.zshared.enumerated.DiversionAuthority;
import com.barangay.zshared.enumerated.FilingType;
import com.barangay.zshared.enumerated.PenaltyRoutingCategory;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing Child in Conflict with the Law (CICL) cases under BCPC.
 * Enforces Philippine Juvenile Justice and Welfare Act (RA 9344 / RA 10630):
 * - Respondent is the minor child alleged of an offense.
 * - Complainant is the victim/offended party or law enforcer.
 * - Discernment, diversion eligibility, and penalty tiering.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BcpcCiclCaseService {

    private final CaseRepository caseRepository;
    private final BcpcCaseDetailsRepository bcpcCaseDetailsRepository;
    private final BcpcCiclAssessmentRepository bcpcCiclAssessmentRepository;
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
     * Files a new Child in Conflict with the Law (CICL) case.
     */
    @Transactional
    public BcpcCiclCaseDetailResponse createCiclCase(BcpcCiclCaseCreateRequest request) {
        log.info("Creating new BCPC Child in Conflict with the Law (CICL) case...");

        Person childPerson = bcpcCommonService.resolveOrCreatePerson(request.child());
        if (childPerson == null) {
            throw new IllegalArgumentException("Child information is required for BCPC cases.");
        }

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

        Person victimPerson = null;
        if (!Boolean.TRUE.equals(request.isVictimlessOffense()) && request.victim() != null) {
            victimPerson = bcpcCommonService.resolveOrCreatePerson(request.victim());
            if (victimPerson != null && childPerson.getId() != null && victimPerson.getId() != null) {
                if (victimPerson.getId().equals(childPerson.getId())) {
                    throw new IllegalArgumentException("The Victim/Offended Party and CICL Child cannot be the same resident in this case.");
                }
            }
        }

        CiclPathway determinedPathway = request.ciclPathway() != null ? request.ciclPathway() : CiclPathway.PENDING;
        DiscernmentStatus discernment = request.discernmentStatus() != null ? request.discernmentStatus() : DiscernmentStatus.PENDING_LSWDO_EVALUATION;
        PenaltyRoutingCategory penaltyRouting = request.penaltyRoutingCategory() != null ? request.penaltyRoutingCategory() : PenaltyRoutingCategory.UNDETERMINED;
        DiversionAuthority authority = request.diversionAuthority() != null ? request.diversionAuthority() : DiversionAuthority.UNDETERMINED;
        boolean isDiversionEligible = Boolean.TRUE.equals(request.isDiversionEligible());

        if (calculatedAge > 0 && calculatedAge <= 15) {
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

        if (victimPerson != null) {
            Complainant complainant = new Complainant();
            complainant.setPerson(victimPerson);
            complainant.setRelationshipToChild(request.victimRelationshipToChild());
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

        String riskLevel = Boolean.TRUE.equals(request.isRepeatOffender()) ? "HIGH" : "MODERATE";
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

        boolean isGuardianAvailable = request.isGuardianAvailable() != null ? request.isGuardianAvailable() : true;
        Person guardianPerson = isGuardianAvailable ? bcpcCommonService.resolveOrCreatePerson(request.guardian()) : null;

        BcpcCaseDetails bcpcDetails = new BcpcCaseDetails();
        bcpcDetails.setCases(savedCase);
        bcpcDetails.setCategory(BcpcCategory.CICL);
        bcpcDetails.setChild(childPerson);
        bcpcDetails.setGradeLevel(request.gradeLevel());
        bcpcDetails.setSchoolName(request.schoolName());
        bcpcDetails.setGuardian(guardianPerson);
        bcpcDetails.setGuardianRelationship(request.guardianRelationship());
        bcpcDetails.setComplainantRole(request.victimRole());
        bcpcDetails.setIsGuardianPresentAtIntake(isGuardianAvailable);
        bcpcDetails.setGuardianUnavailableReason(request.guardianUnavailableReason());
        BcpcCaseDetails savedBcpcDetails = bcpcCaseDetailsRepository.save(bcpcDetails);

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

        caseTimelineService.logEvent(
                savedCase,
                TimelineEventType.CASE_FILED,
                "BCPC Case Filed (CICL)",
                "Child in Conflict with the Law Case #" + savedCase.getCaseNumber() + " was formally filed for minor " + personMapper.buildFullName(childPerson) + ".",
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
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + caseId));

        BcpcCaseDetails details = bcpcCaseDetailsRepository.findByCases(aCase)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC details not found for case ID: " + caseId));

        if (details.getCategory() != BcpcCategory.CICL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Case ID " + caseId + " is not classified as a CICL case.");
        }

        BcpcCiclAssessment cicl = bcpcCiclAssessmentRepository.findByBcpcCaseDetails(details)
                .orElse(null);

        return mapToCiclDetailResponse(aCase, details, cicl);
    }

    /**
     * Updates an existing Child in Conflict with the Law (CICL) case.
     * Preserves minor child and victim identities while selectively updating incident specifics,
     * relationship, legal screening, apprehension info, narrative, evidences, and witnesses.
     */
    @Transactional
    public BcpcCiclCaseDetailResponse updateCiclCase(Long caseId, BcpcCiclCaseUpdateRequest request) {
        log.info("Updating BCPC Child in Conflict with the Law (CICL) case ID: {}", caseId);

        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + caseId));

        if (aCase.getCaseStatus() == CaseStatus.SETTLED || aCase.getCaseStatus() == CaseStatus.DISMISSED || aCase.getCaseStatus() == CaseStatus.WITHDRAWN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot edit settled, dismissed, or withdrawn cases.");
        }

        BcpcCaseDetails details = bcpcCaseDetailsRepository.findByCases(aCase)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC details not found for case ID: " + caseId));

        if (details.getCategory() != BcpcCategory.CICL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Case ID " + caseId + " is not a CICL case.");
        }

        BcpcCiclAssessment cicl = bcpcCiclAssessmentRepository.findByBcpcCaseDetails(details)
                .orElseGet(() -> {
                    BcpcCiclAssessment newCicl = new BcpcCiclAssessment();
                    newCicl.setBcpcCaseDetails(details);
                    return bcpcCiclAssessmentRepository.save(newCicl);
                });

        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            aCase.setCaseUpdatedBy(currentUser);
        }

        // 1. Update Victim / Complainant Relationship to Child & Role
        if (aCase.getComplainant() != null && request.victimRelationshipToChild() != null) {
            aCase.getComplainant().setRelationshipToChild(request.victimRelationshipToChild());
        }
        if (request.victimRole() != null) {
            details.setComplainantRole(request.victimRole());
        }

        // 2. Update Parent / Guardian & Education Status
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

        // 3. Update Incident Details
        if (request.incident() != null) {
            if (aCase.getIncidentDetail() == null) {
                aCase.setIncidentDetail(incidentDetailMapper.fromCreateDto(request.incident()));
            } else {
                IncidentDetail inc = aCase.getIncidentDetail();
                if (request.incident().natureOfComplaint() != null) {
                    String nat = request.incident().natureOfComplaint().trim();
                    if (nat.toUpperCase().startsWith("CICL:")) nat = nat.substring(5).trim();
                    if (nat.toUpperCase().startsWith("CAR:")) nat = nat.substring(4).trim();
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

        // 4. Update CICL Legal Assessment & Screening
        if (request.allegedViolation() != null && !request.allegedViolation().isBlank()) {
            cicl.setAllegedViolation(request.allegedViolation().trim());
        }
        if (request.discernmentStatus() != null) {
            cicl.setDiscernmentStatus(request.discernmentStatus());
        }
        if (request.penaltyRoutingCategory() != null) {
            cicl.setPenaltyRoutingCategory(request.penaltyRoutingCategory());
        }
        if (request.diversionAuthority() != null) {
            cicl.setDiversionAuthority(request.diversionAuthority());
        }
        if (request.admittedOffense() != null) {
            cicl.setAdmittedOffense(request.admittedOffense());
        }
        if (request.isRepeatOffender() != null) {
            cicl.setIsRepeatOffender(request.isRepeatOffender());
        }

        // Apprehension updates
        if (request.apprehendingOfficerName() != null) cicl.setApprehendingOfficerName(request.apprehendingOfficerName());
        if (request.apprehendingOfficerOffice() != null) cicl.setApprehendingOfficerOffice(request.apprehendingOfficerOffice());
        if (request.apprehendingOfficerContact() != null) cicl.setApprehendingOfficerContact(request.apprehendingOfficerContact());
        if (request.apprehensionDateTime() != null) cicl.setApprehensionDateTime(request.apprehensionDateTime());
        if (request.apprehensionPlace() != null) cicl.setApprehensionPlace(request.apprehensionPlace());

        // Re-evaluate legal pathway & diversion eligibility under RA 9344 / RA 10630
        int age = cicl.getAgeAtIncident() != null ? cicl.getAgeAtIncident() : 0;
        if (age > 0 && age <= 15) {
            cicl.setCiclPathway(CiclPathway.INTERVENTION);
            cicl.setIsDiversionEligible(false);
            cicl.setDiscernmentStatus(DiscernmentStatus.WITHOUT_DISCERNMENT);
        } else if (age > 15 && age < 18) {
            if (cicl.getDiscernmentStatus() == DiscernmentStatus.WITHOUT_DISCERNMENT) {
                cicl.setCiclPathway(CiclPathway.INTERVENTION);
                cicl.setIsDiversionEligible(false);
            } else if (cicl.getDiscernmentStatus() == DiscernmentStatus.WITH_DISCERNMENT) {
                cicl.setCiclPathway(CiclPathway.DIVERSION);
                if (cicl.getPenaltyRoutingCategory() == PenaltyRoutingCategory.SIX_YEARS_OR_LESS) {
                    cicl.setIsDiversionEligible(true);
                    if (cicl.getDiversionAuthority() == DiversionAuthority.UNDETERMINED) {
                        cicl.setDiversionAuthority(DiversionAuthority.BARANGAY);
                    }
                } else if (cicl.getPenaltyRoutingCategory() == PenaltyRoutingCategory.ABOVE_SIX_YEARS) {
                    cicl.setIsDiversionEligible(false);
                    if (cicl.getDiversionAuthority() == DiversionAuthority.UNDETERMINED) {
                        cicl.setDiversionAuthority(DiversionAuthority.COURT);
                    }
                }
            }
        }

        // 5. Update Narrative Statement
        if (request.narrative() != null) {
            bcpcCommonService.updateNarrative(aCase, request.narrative());
        }

        // 6. Update Supporting Evidences
        if (request.evidences() != null) {
            bcpcCommonService.updateEvidences(aCase, request.evidences());
        }

        // 7. Update Witnesses
        if (request.witnesses() != null) {
            bcpcCommonService.updateWitnesses(aCase, request.witnesses());
        }

        bcpcCaseDetailsRepository.save(details);
        bcpcCiclAssessmentRepository.save(cicl);
        Case savedCase = caseRepository.save(aCase);

        try {
            caseTimelineService.logEvent(
                    savedCase,
                    TimelineEventType.STATUS_CHANGED,
                    "CICL Case Details Updated",
                    "Child in Conflict with the Law Case #" + savedCase.getCaseNumber() + " details were updated.",
                    currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to log timeline event for CICL update: {}", e.getMessage());
        }

        log.info("BCPC CICL Case #{} updated successfully.", savedCase.getCaseNumber());
        return mapToCiclDetailResponse(savedCase, details, cicl);
    }

    public BcpcCiclCaseDetailResponse mapToCiclDetailResponse(Case aCase, BcpcCaseDetails details, BcpcCiclAssessment cicl) {
        var victimDto = aCase.getComplainant() != null ? complainantMapper.toDetailDto(aCase.getComplainant()) : null;
        String victimRel = aCase.getComplainant() != null ? aCase.getComplainant().getRelationshipToChild() : null;
        var incidentDto = aCase.getIncidentDetail() != null ? incidentDetailMapper.toDetailDto(aCase.getIncidentDetail()) : null;
        if (incidentDto != null && incidentDto.natureOfComplaint() != null) {
            String cleanNat = incidentDto.natureOfComplaint().trim();
            if (cleanNat.toUpperCase().startsWith("CICL:")) cleanNat = cleanNat.substring(5).trim();
            if (cleanNat.toUpperCase().startsWith("CAR:")) cleanNat = cleanNat.substring(4).trim();
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

        String caseStatus = aCase.getCaseStatus() != null ? aCase.getCaseStatus().name() : "PENDING";

        return new BcpcCiclCaseDetailResponse(
                aCase.getId(),
                aCase.getCaseNumber(),
                aCase.getFilingType() != null ? aCase.getFilingType().name() : "FORMAL_COMPLAINT",
                caseStatus,
                aCase.getCaseStatusRemarks(),
                aCase.getRiskLevel(),
                aCase.getCreatedAt(),
                reportingOfficer,
                assignedOfficer,

                victimDto,
                aCase.getComplainant() == null,
                details != null ? details.getComplainantRole() : null,
                victimRel,

                bcpcCommonService.toPersonDetailDto(details != null ? details.getChild() : null),
                details != null ? details.getGradeLevel() : null,
                details != null ? details.getSchoolName() : null,

                bcpcCommonService.toPersonDetailDto(details != null ? details.getGuardian() : null),
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
}
