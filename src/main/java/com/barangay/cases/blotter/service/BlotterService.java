package com.barangay.cases.blotter.service;

import com.barangay.cases.blotter.dto.BlotterCaseCreateRequest;
import com.barangay.cases.blotter.dto.BlotterCaseDetailResponse;
import com.barangay.cases.sharedmodel.*;
import com.barangay.cases.sharedmodel.repository.CaseRepository;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.people.witness.model.Witness;
import com.barangay.zshared.dto.*;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import com.barangay.zshared.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private final ComplainantMapper complainantMapper;
    private final RespondentMapper respondentMapper;
    private final IncidentDetailMapper incidentDetailMapper;
    private final NarrativeStatementMapper narrativeStatementMapper;
    private final CaseEvidenceMapper caseEvidenceMapper;
    private final WitnessMapper witnessMapper;

    /**
     * Creates a new Blotter Case (Formal Complaint or For the Record).
     *
     * @param request the case creation payload containing complainant, respondent, incident details, narrative, and witnesses
     * @return the created case details mapped to {@link BlotterCaseDetailResponse}
     */
    @Transactional
    public BlotterCaseDetailResponse createBlotterCase(BlotterCaseCreateRequest request) {
        Case aCase = new Case();

        // 1. Filing Type & Case Number & Status
        FilingType filingType = request.filingType() != null ? request.filingType() : FilingType.FORMAL_COMPLAINT;
        aCase.setFilingType(filingType);

        String caseNumber = request.caseNumber();
        if (caseNumber == null || caseNumber.isBlank()) {
            caseNumber = generateCaseNumber();
        }
        aCase.setCaseNumber(caseNumber);
        aCase.setCaseStatus(CaseStatus.PENDING);
        aCase.setIsArchived(false);
        aCase.setIsAttested(request.isCertified() != null ? request.isCertified() : false);

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

        // 6. Assigned Officer lookup
        if (request.assignedOfficerId() != null) {
            officerRepository.findById(request.assignedOfficerId()).ifPresent(aCase::setOfficer);
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

        // 9. Map to Response DTO
        return toDetailResponse(savedCase, request.receivingOfficer(), request.assignedOfficer());
    }

    /**
     * Generates a default unique case number if none is supplied.
     */
    private String generateCaseNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = String.format("%04d", (int) (Math.random() * 10000));
        return "BC-" + datePart + "-" + uniquePart;
    }

    /**
     * Maps a saved {@link Case} entity to {@link BlotterCaseDetailResponse}.
     */
    private BlotterCaseDetailResponse toDetailResponse(Case aCase, String receivingOfficerName, String assignedOfficerName) {
        ComplainantDetailDto complainantDto = complainantMapper.toDetailDto(aCase.getComplainant());
        RespondentDetailDto respondentDto = respondentMapper.toDetailDto(aCase.getRespondent());
        IncidentDetailDto incidentDto = incidentDetailMapper.toDetailDto(aCase.getIncidentDetail());
        NarrativeDetailDto narrativeDto = narrativeStatementMapper.toDetailDto(aCase.getNarrativeStatement());

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
                null,
                witnessDtos,
                aCase.getIsAttested(),
                aCase.getCreatedAt(),
                aCase.getUpdatedAt()
        );
    }

}

