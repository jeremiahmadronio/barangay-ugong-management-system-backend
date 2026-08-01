package com.barangay.cases.blotter.service;

import com.barangay.cases.blotter.dto.BlotterCaseCreateRequest;
import com.barangay.cases.blotter.dto.BlotterCaseResponse;
import com.barangay.cases.blotter.dto.BlotterStatsResponse;
import com.barangay.cases.sharedmodel.Case;
import com.barangay.cases.sharedmodel.IncidentDetail;
import com.barangay.cases.sharedmodel.NarrativeStatement;
import com.barangay.cases.sharedmodel.repository.CaseRepository;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.person.model.Person;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.people.witness.model.Witness;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import com.barangay.zshared.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barangay.people.person.repository.PersonRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Business logic service for managing Blotter Cases (Formal Complaint and For the Record).
 */
@Service
@RequiredArgsConstructor
public class BlotterService {

    private final CaseRepository caseRepository;
    private final PersonRepository personRepository;

    /**
     * Creates and saves a new Blotter Case entry (Formal Complaint or For the Record).
     */
    @Transactional
    public BlotterCaseResponse createBlotterCase(BlotterCaseCreateRequest request) {
        Case newCase = new Case();

        // 1. Filing Type & Case Number Generation
        FilingType type = request.filingType() != null ? request.filingType() : FilingType.FORMAL_COMPLAINT;
        newCase.setFilingType(type);

        if (request.caseNumber() != null && !request.caseNumber().isBlank()) {
            newCase.setCaseNumber(request.caseNumber());
        } else {
            String prefix = (type == FilingType.FOR_RECORD_ONLY) ? "2026-REC-" : "2026-BLT-";
            String randomDigits = String.format("%03d", new Random().nextInt(900) + 100);
            newCase.setCaseNumber(prefix + randomDigits);
        }

        newCase.setCaseStatus(CaseStatus.PENDING);
        newCase.setIsArchived(false);

        // 2. Map Complainant (Person -> Complainant)
        if (request.complainant() != null) {
            Person compPerson = new Person();
            compPerson.setLastName(request.complainant().lastName());
            compPerson.setFirstName(request.complainant().firstName());
            compPerson.setMiddleName(request.complainant().middleName());
            compPerson.setContactNumber(request.complainant().contactNumber());
            compPerson.setAge(request.complainant().age());
            compPerson.setGender(request.complainant().gender());
            compPerson.setCivilStatus(request.complainant().civilStatus());
            compPerson.setEmail(request.complainant().email());

            String fullAddr = buildFullAddress(
                    request.complainant().houseStreet(),
                    request.complainant().barangay(),
                    request.complainant().city(),
                    request.complainant().province(),
                    request.complainant().completeAddress()
            );
            compPerson.setCompleteAddress(fullAddr);

            compPerson = personRepository.save(compPerson);

            Complainant complainant = new Complainant();
            complainant.setPerson(compPerson);
            newCase.setComplainant(complainant);
        }

        // 3. Map Respondent (Person -> Respondent)
        if (request.respondent() != null) {
            Person respPerson = new Person();
            respPerson.setLastName(request.respondent().lastName());
            respPerson.setFirstName(request.respondent().firstName());
            respPerson.setMiddleName(request.respondent().middleName());
            respPerson.setContactNumber(request.respondent().contactNumber());
            respPerson.setAge(request.respondent().age());
            respPerson.setGender(request.respondent().gender());

            String fullAddr = buildFullAddress(
                    request.respondent().houseStreet(),
                    request.respondent().barangay(),
                    request.respondent().city(),
                    request.respondent().province(),
                    request.respondent().completeAddress()
            );
            respPerson.setCompleteAddress(fullAddr);

            respPerson = personRepository.save(respPerson);

            Respondent respondent = new Respondent();
            respondent.setPerson(respPerson);
            respondent.setRelationshipToComplainant(request.respondent().relationshipToComplainant());
            respondent.setLivingWithComplainant(request.respondent().currentlyLivingWithComplainant());
            newCase.setRespondent(respondent);
        }

        // 4. Map Incident Details
        if (request.incident() != null) {
            IncidentDetail incidentDetail = new IncidentDetail();
            incidentDetail.setNatureOfComplaint(request.incident().natureOfComplaint());
            incidentDetail.setFrequency(request.incident().frequencyOfIncident());
            incidentDetail.setIncidentLocation(request.incident().placeOfIncident());
            incidentDetail.setInjuriesDamagesDescription(request.incident().descriptionOfInjuries());

            if (request.incident().dateOfIncident() != null && !request.incident().dateOfIncident().isBlank()) {
                try {
                    incidentDetail.setDateOfIncident(LocalDate.parse(request.incident().dateOfIncident()));
                } catch (Exception ignored) {}
            }

            if (request.incident().timeOfIncident() != null && !request.incident().timeOfIncident().isBlank()) {
                try {
                    incidentDetail.setTimeOfIncident(LocalTime.parse(request.incident().timeOfIncident()));
                } catch (Exception ignored) {}
            }

            newCase.setIncidentDetail(incidentDetail);
        }

        // 5. Map Narrative Statement & Statement Summary
        String textToSave = request.statementSummary() != null && !request.statementSummary().isBlank()
                ? request.statementSummary()
                : request.statementText();

        if (textToSave != null && !textToSave.isBlank()) {
            NarrativeStatement narrative = new NarrativeStatement();
            narrative.setFileName("StatementText.txt");
            narrative.setStatementSummary(textToSave);
            narrative.setFileData(textToSave.getBytes());
            newCase.setNarrativeStatement(narrative);
        }

        // 6. Map Witnesses
        if (request.witnesses() != null && !request.witnesses().isEmpty()) {
            for (BlotterCaseCreateRequest.WitnessDto wDto : request.witnesses()) {
                if (wDto.firstName() == null && wDto.lastName() == null) continue;

                Person wPerson = new Person();
                wPerson.setFirstName(wDto.firstName() != null ? wDto.firstName() : "Witness");
                wPerson.setLastName(wDto.lastName() != null ? wDto.lastName() : "N/A");
                wPerson.setContactNumber(wDto.contactNumber());
                wPerson.setCompleteAddress(buildFullAddress(wDto.houseStreet(), wDto.barangay(), wDto.city(), wDto.province(), wDto.completeAddress()));

                wPerson = personRepository.save(wPerson);

                Witness witness = new Witness();
                witness.setACase(newCase);
                witness.setPerson(wPerson);
                witness.setTestimony(wDto.testimonyText());
                newCase.getWitnesses().add(witness);
            }
        }

        Case savedCase = caseRepository.save(newCase);
        return mapToResponse(savedCase);
    }


    @Transactional(readOnly = true)
    public List<BlotterCaseResponse> getAllBlotterCases(String search, String status, String natureOfComplaint, String filingType) {
        List<Case> cases = caseRepository.findFilteredCasesList(search, status, natureOfComplaint, filingType);
        return cases.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<BlotterCaseResponse> getBlotterCasesPage(String search, String status, String natureOfComplaint, String filingType, org.springframework.data.domain.Pageable pageable) {
        org.springframework.data.domain.Page<Case> casesPage = caseRepository.findFilteredCases(search, status, natureOfComplaint, filingType, pageable);
        return casesPage.map(this::mapToResponse);
    }

    /**
     * Gets a single Blotter Case detail by ID.
     */
    @Transactional(readOnly = true)
    public BlotterCaseResponse getBlotterCaseById(Long id) {
        Case c = caseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blotter Case not found with ID: " + id));
        return mapToResponse(c);
    }

    /**
     * Updates status of a Blotter Case.
     */
    @Transactional
    public BlotterCaseResponse updateCaseStatus(Long id, CaseStatus status, String remarks) {
        Case c = caseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blotter Case not found with ID: " + id));
        c.setCaseStatus(status);
        if (remarks != null) {
            c.setCaseStatusRemarks(remarks);
        }
        Case updated = caseRepository.save(c);
        return mapToResponse(updated);
    }

    /**
     * Deletes (archives) a Blotter Case by ID.
     * Enforces business rule: Cannot archive cases that are PENDING, UNDER_MEDIATION, or UNDER_CONCILIATION.
     */
    @Transactional
    public void deleteBlotterCase(Long id) {
        Case c = caseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Blotter Case not found with ID: " + id));

        if (c.getCaseStatus() == CaseStatus.PENDING ||
            c.getCaseStatus() == CaseStatus.UNDER_MEDIATION ||
            c.getCaseStatus() == CaseStatus.UNDER_CONCILIATION) {
            throw new BusinessRuleException(
                "Cannot archive Case #" + c.getCaseNumber() + " while its status is " + c.getCaseStatus().name() +
                ". Only SETTLED, DISMISSED, or WITHDRAWN cases can be archived."
            );
        }

        c.setIsArchived(true);
        caseRepository.save(c);
    }

    /**
     * Calculates Blotter dashboard summary statistics separated for Formal Complaint vs For the Record.
     */
    @Transactional(readOnly = true)
    public BlotterStatsResponse getBlotterStats() {
        List<Case> cases = caseRepository.findAll();

        java.time.LocalDateTime startOfCurrentMonth = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();
        java.time.LocalDateTime startOfPrevMonth = startOfCurrentMonth.minusMonths(1);

        long currentMonthCases = cases.stream().filter(c -> Boolean.FALSE.equals(c.getIsArchived()) && c.getCreatedAt() != null && !c.getCreatedAt().isBefore(startOfCurrentMonth)).count();
        long previousMonthCases = cases.stream().filter(c -> Boolean.FALSE.equals(c.getIsArchived()) && c.getCreatedAt() != null && !c.getCreatedAt().isBefore(startOfPrevMonth) && c.getCreatedAt().isBefore(startOfCurrentMonth)).count();

        long currentMonthRecord = cases.stream().filter(c -> Boolean.FALSE.equals(c.getIsArchived()) && (c.getFilingType() == null || c.getFilingType() == FilingType.FOR_RECORD_ONLY) && c.getCreatedAt() != null && !c.getCreatedAt().isBefore(startOfCurrentMonth)).count();
        long previousMonthRecord = cases.stream().filter(c -> Boolean.FALSE.equals(c.getIsArchived()) && (c.getFilingType() == null || c.getFilingType() == FilingType.FOR_RECORD_ONLY) && c.getCreatedAt() != null && !c.getCreatedAt().isBefore(startOfPrevMonth) && c.getCreatedAt().isBefore(startOfCurrentMonth)).count();

        long total = cases.stream().filter(c -> Boolean.FALSE.equals(c.getIsArchived())).count();
        long pending = cases.stream().filter(c -> Boolean.FALSE.equals(c.getIsArchived()) && c.getCaseStatus() == CaseStatus.PENDING).count();

        long underMediation = cases.stream().filter(c -> Boolean.FALSE.equals(c.getIsArchived()) && (c.getCaseStatus() == CaseStatus.UNDER_MEDIATION || c.getCaseStatus() == CaseStatus.UNDER_CONCILIATION)).count();
        long settled = cases.stream().filter(c -> Boolean.FALSE.equals(c.getIsArchived()) && (c.getCaseStatus() == CaseStatus.SETTLED)).count();

        // Formal Complaint Stats
        long totalFormal = cases.stream().filter(c -> Boolean.FALSE.equals(c.getIsArchived()) && c.getFilingType() == FilingType.FORMAL_COMPLAINT).count();
        long pendingFormal = cases.stream().filter(c -> Boolean.FALSE.equals(c.getIsArchived()) && c.getFilingType() == FilingType.FORMAL_COMPLAINT && c.getCaseStatus() == CaseStatus.PENDING).count();
        long mediationFormal = cases.stream().filter(c -> Boolean.FALSE.equals(c.getIsArchived()) && c.getFilingType() == FilingType.FORMAL_COMPLAINT && (c.getCaseStatus() == CaseStatus.UNDER_MEDIATION || c.getCaseStatus() == CaseStatus.UNDER_CONCILIATION)).count();
        long settledFormal = cases.stream().filter(c -> Boolean.FALSE.equals(c.getIsArchived()) && c.getFilingType() == FilingType.FORMAL_COMPLAINT && (c.getCaseStatus() == CaseStatus.SETTLED)).count();

        // For the Record Stats
        long totalRecord = cases.stream().filter(c -> Boolean.FALSE.equals(c.getIsArchived()) && (c.getFilingType() == null || c.getFilingType() == FilingType.FOR_RECORD_ONLY)).count();
        long incidentOnlyRecord = cases.stream().filter(c -> Boolean.FALSE.equals(c.getIsArchived()) && (c.getFilingType() == null || c.getFilingType() == FilingType.FOR_RECORD_ONLY) && c.getIncidentDetail() != null && c.getIncidentDetail().getNatureOfComplaint() != null && c.getIncidentDetail().getNatureOfComplaint().toLowerCase().contains("incident")).count();
        long consultationRecord = cases.stream().filter(c -> Boolean.FALSE.equals(c.getIsArchived()) && (c.getFilingType() == null || c.getFilingType() == FilingType.FOR_RECORD_ONLY) && c.getIncidentDetail() != null && c.getIncidentDetail().getNatureOfComplaint() != null && (c.getIncidentDetail().getNatureOfComplaint().toLowerCase().contains("consultation") || c.getIncidentDetail().getNatureOfComplaint().toLowerCase().contains("legal"))).count();
        long recentRecord = totalRecord;

        String totalCasesTrend = calculateTrend(currentMonthCases, previousMonthCases);
        String totalForTheRecordTrend = calculateTrend(currentMonthRecord, previousMonthRecord);
        String underMediationTrend = "0.0% vs last month";
        String settledCasesTrend = "0.0% vs last month";

        return new BlotterStatsResponse(
                total, pending, underMediation, settled,
                totalFormal, pendingFormal, mediationFormal, settledFormal,
                totalRecord, incidentOnlyRecord > 0 ? incidentOnlyRecord : totalRecord, consultationRecord, recentRecord,
                totalCasesTrend, totalForTheRecordTrend, underMediationTrend, settledCasesTrend
        );
    }

    private String calculateTrend(long currentCount, long previousCount) {
        if (previousCount == 0) {
            return currentCount > 0 ? "+100.0% vs last month" : "0.0% vs last month";
        }
        double diff = ((double) (currentCount - previousCount) / (double) previousCount) * 100.0;
        double rounded = Math.round(diff * 10.0) / 10.0;
        if (rounded > 0) {
            return String.format("+%.1f%% vs last month", rounded);
        } else if (rounded < 0) {
            return String.format("%.1f%% vs last month", rounded);
        } else {
            return "0.0% vs last month";
        }
    }

    private String buildFullAddress(String hs, String brgy, String city, String prov, String fallback) {
        if (fallback != null && !fallback.isBlank()) return fallback;
        StringBuilder sb = new StringBuilder();
        if (hs != null && !hs.isBlank()) sb.append(hs).append(", ");
        if (brgy != null && !brgy.isBlank()) sb.append("Brgy. ").append(brgy).append(", ");
        if (city != null && !city.isBlank()) sb.append(city).append(", ");
        if (prov != null && !prov.isBlank()) sb.append(prov);
        String res = sb.toString().trim();
        return res.endsWith(",") ? res.substring(0, res.length() - 1) : res;
    }

    private BlotterCaseResponse mapToResponse(Case c) {
        String compName = "";
        String compContact = "";
        String compAddr = "";
        if (c.getComplainant() != null && c.getComplainant().getPerson() != null) {
            Person p = c.getComplainant().getPerson();
            compName = (p.getFirstName() + " " + p.getLastName()).trim();
            compContact = p.getContactNumber();
            compAddr = p.getCompleteAddress();
        }

        String respName = "";
        String respContact = "";
        String respAddr = "";
        String relToComp = "";
        if (c.getRespondent() != null && c.getRespondent().getPerson() != null) {
            Person p = c.getRespondent().getPerson();
            respName = (p.getFirstName() + " " + p.getLastName()).trim();
            respContact = p.getContactNumber();
            respAddr = p.getCompleteAddress();
            relToComp = c.getRespondent().getRelationshipToComplainant();
        }

        String nature = "";
        String dateInc = "";
        String timeInc = "";
        String locInc = "";
        String injDam = "";
        if (c.getIncidentDetail() != null) {
            IncidentDetail inc = c.getIncidentDetail();
            nature = inc.getNatureOfComplaint();
            if (inc.getDateOfIncident() != null) dateInc = inc.getDateOfIncident().toString();
            if (inc.getTimeOfIncident() != null) timeInc = inc.getTimeOfIncident().toString();
            locInc = inc.getIncidentLocation();
            injDam = inc.getInjuriesDamagesDescription();
        }

        String stmt = "";
        if (c.getNarrativeStatement() != null) {
            if (c.getNarrativeStatement().getStatementSummary() != null && !c.getNarrativeStatement().getStatementSummary().isBlank()) {
                stmt = c.getNarrativeStatement().getStatementSummary();
            } else if (c.getNarrativeStatement().getFileData() != null) {
                stmt = new String(c.getNarrativeStatement().getFileData());
            }
        }

        String dateFiledFormatted = "";
        if (c.getCreatedAt() != null) {
            dateFiledFormatted = c.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        } else if (c.getIncidentDetail() != null && c.getIncidentDetail().getDateOfIncident() != null) {
            dateFiledFormatted = c.getIncidentDetail().getDateOfIncident().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        }

        return new BlotterCaseResponse(
                c.getId(),
                c.getCaseNumber(),
                c.getFilingType(),
                c.getCaseStatus(),
                compName,
                compContact,
                compAddr,
                respName,
                respContact,
                respAddr,
                relToComp,
                nature,
                dateInc,
                timeInc,
                locInc,
                injDam,
                stmt,
                stmt,
                c.getCaseFilledBy() != null ? c.getCaseFilledBy().getUsername() : null,
                c.getOfficer() != null && c.getOfficer().getPerson() != null ? c.getOfficer().getPerson().getFirstName() : null,
                c.getWitnesses() != null ? c.getWitnesses().size() : 0,
                c.getCreatedAt(),
                c.getUpdatedAt(),
                dateFiledFormatted
        );
    }
}
