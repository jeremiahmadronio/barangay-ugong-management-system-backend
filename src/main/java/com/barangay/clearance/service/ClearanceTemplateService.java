package com.barangay.clearance.service;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.IncidentDetail;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.clearance.dto.*;
import com.barangay.clearance.model.CertificateTemplate;
import com.barangay.clearance.model.IssuedCertificate;
import com.barangay.clearance.repository.CertificateTemplateRepository;
import com.barangay.clearance.repository.IssuedCertificateRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.enumerated.ClearanceStatus;
import com.barangay.zshared.security.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service handling clearance templates, applicant resident verification, and issuance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClearanceTemplateService {

    private final CertificateTemplateRepository certificateTemplateRepository;
    private final ResidentRepository residentRepository;
    private final PersonRepository personRepository;
    private final IssuedCertificateRepository issuedCertificateRepository;
    private final CaseRepository caseRepository;

    /**
     * Gets document options for active certificate templates.
     */
    @Transactional(readOnly = true)
    public List<TemplateOptionResponse> getTemplateOptions() {
        return certificateTemplateRepository.findAllByIsArchivedFalseAndIsDeletedFalse()
                .stream()
                .map(t -> new TemplateOptionResponse(t.getId(), t.getCertTitle()))
                .toList();
    }

    /**
     * Gets complete template details by ID for the issuance form.
     */
    @Transactional(readOnly = true)
    public TemplateDetailResponse getTemplateById(Long id) {
        CertificateTemplate template = certificateTemplateRepository.findById(id)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate template not found"));

        return new TemplateDetailResponse(
                template.getId(),
                template.getCertTitle(),
                template.getBodySections(),
                template.getIssueFields(),
                template.isRequiresPhoto(),
                template.isRequiresThumbmark(),
                template.getValidityMonths(),
                template.getFooterText(),
                template.getIsArchived()
        );
    }

    /**
     * Searches active residents and resolves demographic profile, issuance history, and multi-department case records.
     */
    @Transactional(readOnly = true)
    public List<ClearanceResidentSearchResponse> searchResidents(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }

        List<Resident> residents = residentRepository.searchActiveResidents(query.trim());
        return residents.stream()
                .filter(r -> r.getPerson() != null)
                .map(this::buildResidentClearanceProfile)
                .toList();
    }

    /**
     * Gets a single resident's clearance profile by Resident ID.
     */
    @Transactional(readOnly = true)
    public ClearanceResidentSearchResponse getResidentClearanceProfile(Long residentId) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resident not found with id: " + residentId));
        return buildResidentClearanceProfile(resident);
    }

    private ClearanceResidentSearchResponse buildResidentClearanceProfile(Resident resident) {
        Person person = resident.getPerson();

        String firstName = null;
        String middleName = null;
        String lastName = null;
        String suffix = null;
        String fullName = null;
        Integer age = null;
        String gender = null;
        String civilStatus = null;
        String contactNumber = null;
        String completeAddress = null;
        LocalDate birthDate = null;
        String placeOfBirth = null;
        String residencyDate = null;

        if (person != null) {
            firstName = person.getFirstName();
            middleName = person.getMiddleName();
            lastName = person.getLastName();
            suffix = person.getSuffix();
            fullName = buildFullName(firstName, middleName, lastName, suffix);

            int calcAge = person.getCalculatedAge();
            if (calcAge > 0) {
                age = calcAge;
            } else if (person.getAge() != null) {
                age = (int) person.getAge();
            } else if (person.getBirthDate() != null) {
                age = Period.between(person.getBirthDate(), LocalDate.now()).getYears();
            }

            gender = person.getGender();
            civilStatus = person.getCivilStatus();
            contactNumber = person.getContactNumber();
            completeAddress = person.getCompleteAddress();
            birthDate = person.getBirthDate();
            placeOfBirth = "Valenzuela City";
        }

        if (resident.getDateOfResidency() != null) {
            residencyDate = resident.getDateOfResidency().toString();
        }

        // 1. Previous Clearance Issuance History
        List<ClearanceIssuanceHistoryResponse> issuanceHistory = List.of();
        Long pId = person != null ? person.getId() : null;
        if (pId != null || (fullName != null && !fullName.trim().isEmpty())) {
            List<IssuedCertificate> certs =
                    issuedCertificateRepository.findActiveCertificatesByPersonOrName(pId, fullName != null ? fullName.trim() : "");
            issuanceHistory = certs.stream().map(c -> new ClearanceIssuanceHistoryResponse(
                    c.getCertNumber(),
                    c.getTemplate() != null ? c.getTemplate().getCertTitle() : "BARANGAY CLEARANCE",
                    c.getCreatedAt() != null ? c.getCreatedAt() : LocalDateTime.now(),
                    c.getStatus() != null ? c.getStatus().name() : "ACTIVE"
            )).toList();
        }

        // 2. Barangay Case History (BCPC, VAWC, Lupon, Blotter - non-deleted, non-archived)
        List<ClearanceCaseHistoryResponse> caseHistoryList = new ArrayList<>();
        if (person != null && person.getId() != null) {
            String pFirst = person.getFirstName() != null ? person.getFirstName().trim() : null;
            String pLast = person.getLastName() != null ? person.getLastName().trim() : null;

            List<Case> rawCases = caseRepository.findByPersonInCaseRoles(person.getId(), pFirst, pLast);

            // Deduplicate reopened case chains
            Set<String> supersededCaseNumbers = new HashSet<>();
            for (Case c : rawCases) {
                if (c.getReOpenedCaseNumber() != null && !c.getReOpenedCaseNumber().trim().isEmpty()) {
                    supersededCaseNumbers.add(c.getCaseNumber());
                }
            }

            for (Case c : rawCases) {
                if (supersededCaseNumbers.contains(c.getCaseNumber())) {
                    continue;
                }

                String role = "OTHER";
                boolean isComp = c.getComplainant() != null && isSamePerson(person, c.getComplainant().getPerson());
                boolean isResp = c.getRespondent() != null && isSamePerson(person, c.getRespondent().getPerson());
                boolean isWitness = c.getWitnesses() != null && c.getWitnesses().stream().anyMatch(w -> isSamePerson(person, w.getPerson()));

                if (!isComp && !isResp && !isWitness) {
                    continue;
                }

                if (isComp) {
                    role = "COMPLAINANT";
                } else if (isResp) {
                    role = "RESPONDENT";
                } else if (isWitness) {
                    role = "WITNESS";
                }

                String incidentNature = "General Complaint";
                IncidentDetail detail = c.getIncidentDetail();
                if (detail != null && detail.getNatureOfComplaint() != null && !detail.getNatureOfComplaint().trim().isEmpty()) {
                    incidentNature = detail.getNatureOfComplaint();
                }

                caseHistoryList.add(new ClearanceCaseHistoryResponse(
                        c.getCaseNumber() != null ? c.getCaseNumber() : "CASE-" + c.getId(),
                        incidentNature,
                        role,
                        c.getCreatedAt(),
                        c.getCaseStatus() != null ? c.getCaseStatus().name() : "PENDING"
                ));
            }
        }

        return new ClearanceResidentSearchResponse(
                resident.getResidentId(),
                person != null ? person.getId() : null,
                firstName,
                middleName,
                lastName,
                suffix,
                fullName,
                age,
                gender,
                civilStatus,
                contactNumber,
                completeAddress,
                birthDate,
                placeOfBirth,
                residencyDate,
                issuanceHistory,
                caseHistoryList
        );
    }

    private boolean isSamePerson(Person target, Person candidate) {
        if (target == null || candidate == null) return false;
        if (target.getId() != null && candidate.getId() != null && target.getId().equals(candidate.getId())) {
            return true;
        }
        if (target.getFirstName() == null || target.getLastName() == null ||
                candidate.getFirstName() == null || candidate.getLastName() == null) {
            return false;
        }
        return target.getFirstName().trim().equalsIgnoreCase(candidate.getFirstName().trim()) &&
                target.getLastName().trim().equalsIgnoreCase(candidate.getLastName().trim());
    }

    /**
     * Issues a new clearance certificate and generates the unique tracking number.
     */
    @Transactional
    public IssueClearanceResponse issueClearance(IssueClearanceRequest request) {
        if (request.templateId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template ID is required");
        }

        CertificateTemplate template = certificateTemplateRepository.findById(request.templateId())
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificate template not found"));

        Person person = null;
        if (request.personId() != null) {
            person = personRepository.findById(request.personId()).orElse(null);
            if (person == null) {
                person = residentRepository.findById(request.personId())
                        .map(Resident::getPerson)
                        .orElse(null);
            }
        }

        String reqName = request.requestorName();
        if ((reqName == null || reqName.trim().isEmpty()) && person != null) {
            reqName = buildFullName(person.getFirstName(), person.getMiddleName(), person.getLastName(), person.getSuffix());
        }
        if (reqName == null || reqName.trim().isEmpty()) {
            reqName = "Resident Applicant";
        }

        if (person == null && reqName != null && !reqName.trim().isEmpty() && !reqName.equalsIgnoreCase("Resident Applicant")) {
            List<Resident> found = residentRepository.searchActiveResidents(reqName.trim());
            if (!found.isEmpty() && found.get(0).getPerson() != null) {
                person = found.get(0).getPerson();
            }
        }

        // If person is still null (e.g. walk-in applicant without resident record), create a Person entry
        if (person == null && request.fieldValues() != null) {
            JsonNode fields = request.fieldValues();
            String fName = fields.hasNonNull("firstName") ? fields.get("firstName").asText() : null;
            String lName = fields.hasNonNull("lastName") ? fields.get("lastName").asText() : null;
            if (fName != null && lName != null && !fName.trim().isEmpty() && !lName.trim().isEmpty()) {
                if (fName.trim().length() > 50) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First name cannot exceed 50 characters");
                }
                if (lName.trim().length() > 50) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Last name cannot exceed 50 characters");
                }
                Person newPerson = new Person();
                newPerson.setFirstName(fName.trim());
                if (fields.hasNonNull("middleName")) {
                    String mName = fields.get("middleName").asText().trim();
                    if (mName.length() > 50) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Middle name cannot exceed 50 characters");
                    }
                    newPerson.setMiddleName(mName);
                }
                newPerson.setLastName(lName.trim());
                if (fields.hasNonNull("suffix")) newPerson.setSuffix(fields.get("suffix").asText().trim());
                if (fields.hasNonNull("address")) newPerson.setCompleteAddress(fields.get("address").asText().trim());
                if (fields.hasNonNull("gender")) newPerson.setGender(fields.get("gender").asText().trim());
                if (fields.hasNonNull("civilStatus")) newPerson.setCivilStatus(fields.get("civilStatus").asText().trim());
                if (fields.hasNonNull("contactNumber")) newPerson.setContactNumber(fields.get("contactNumber").asText().trim());
                if (fields.hasNonNull("dateOfBirth")) {
                    try {
                        LocalDate dob = LocalDate.parse(fields.get("dateOfBirth").asText());
                        if (dob.isAfter(LocalDate.now())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date of birth cannot be in the future");
                        }
                        if (dob.isBefore(LocalDate.now().minusYears(130))) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Age cannot exceed 130 years");
                        }
                        newPerson.setBirthDate(dob);
                    } catch (ResponseStatusException rse) {
                        throw rse;
                    } catch (Exception ignored) {}
                }
                person = personRepository.save(newPerson);
            }
        }

        int year = LocalDate.now().getYear();
        String prefix = generateTemplatePrefix(template.getCertTitle());
        long countThisYear = issuedCertificateRepository.count();
        String certNumber = String.format("%s-%d-%05d", prefix, year, countThisYear + 1);

        while (issuedCertificateRepository.existsByCertNumber(certNumber)) {
            countThisYear++;
            certNumber = String.format("%s-%d-%05d", prefix, year, countThisYear + 1);
        }

        IssuedCertificate cert = new IssuedCertificate();
        cert.setCertNumber(certNumber);
        cert.setTemplate(template);
        cert.setPerson(person);
        cert.setRequestorName(reqName.trim());
        cert.setFieldValues(request.fieldValues());
        cert.setOrNumber(request.orNumber());
        cert.setCtnNumber(request.ctnNumber());
        cert.setStatus(ClearanceStatus.ACTIVE);

        int validityMonths = template.getValidityMonths() != null ? template.getValidityMonths() : 6;
        cert.setExpiryDate(LocalDate.now().plusMonths(validityMonths));

        User currentUser = SecurityUtils.getCurrentUser();
        cert.setIssuedBy(currentUser);
        cert.setIsArchived(false);
        cert.setIsDeleted(false);
        cert.setIsPermanentDeleted(false);

        IssuedCertificate saved = issuedCertificateRepository.save(cert);
        log.info("Successfully issued clearance certificate: {} for requestor: {}", certNumber, reqName);

        return new IssueClearanceResponse(true, "Clearance certificate issued successfully.", certNumber, saved.getId());
    }

    private String generateTemplatePrefix(String certTitle) {
        if (certTitle == null || certTitle.trim().isEmpty()) return "CLR";
        String upper = certTitle.toUpperCase();
        if (upper.contains("BUILDING")) return "BC";
        if (upper.contains("BAIL")) return "BAIL";
        if (upper.contains("DECEASED")) return "DEC";
        if (upper.contains("INDIGENCY")) return "IND";
        if (upper.contains("UTILITY") || upper.contains("ELECTRICAL")) return "UTIL";
        if (upper.contains("EBIKE") || upper.contains("ETRIKE")) return "EBK";
        if (upper.contains("BARANGAY")) return "BC";
        return "CLR";
    }

    private String buildFullName(String first, String middle, String last, String suffix) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.trim().isEmpty()) sb.append(first.trim());
        if (middle != null && !middle.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(middle.trim());
        }
        if (last != null && !last.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(last.trim());
        }
        if (suffix != null && !suffix.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(suffix.trim());
        }
        return sb.toString();
    }
}
