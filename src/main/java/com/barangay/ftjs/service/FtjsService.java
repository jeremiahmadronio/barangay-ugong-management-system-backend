package com.barangay.ftjs.service;

import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.ftjs.dto.FtjsBarangayCaseHistoryResponse;
import com.barangay.ftjs.dto.FtjsIssuanceHistoryResponse;
import com.barangay.ftjs.dto.FtjsResidentSearchResponse;
import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.zshared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for FTJS resident search and unified response mapping (Demographics, Issuance History, Case History).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FtjsService {

    private final FirstTimeJobSeekerRepository ftjsRepository;
    private final ResidentRepository residentRepository;
    private final CaseRepository caseRepository;

    /**
     * Searches active residents by name/query and builds unified FtjsResidentSearchResponse data.
     */
    @Transactional(readOnly = true)
    public List<FtjsResidentSearchResponse> searchResidents(String query) {
        String searchTerm = (query != null) ? query.trim().toLowerCase() : "";
        List<Resident> residents = residentRepository.searchActiveResidents(searchTerm);

        return residents.stream()
                .sorted((r1, r2) -> {
                    if (searchTerm.isEmpty()) return 0;

                    String f1 = (r1.getPerson() != null && r1.getPerson().getFirstName() != null) ? r1.getPerson().getFirstName().toLowerCase() : "";
                    String l1 = (r1.getPerson() != null && r1.getPerson().getLastName() != null) ? r1.getPerson().getLastName().toLowerCase() : "";
                    String f2 = (r2.getPerson() != null && r2.getPerson().getFirstName() != null) ? r2.getPerson().getFirstName().toLowerCase() : "";
                    String l2 = (r2.getPerson() != null && r2.getPerson().getLastName() != null) ? r2.getPerson().getLastName().toLowerCase() : "";

                    boolean f1Starts = f1.startsWith(searchTerm);
                    boolean f2Starts = f2.startsWith(searchTerm);
                    if (f1Starts && !f2Starts) return -1;
                    if (!f1Starts && f2Starts) return 1;

                    boolean l1Starts = l1.startsWith(searchTerm);
                    boolean l2Starts = l2.startsWith(searchTerm);
                    if (l1Starts && !l2Starts) return -1;
                    if (!l1Starts && l2Starts) return 1;

                    return f1.compareTo(f2);
                })
                .map(this::buildFtjsResponse)
                .toList();
    }

    /**
     * Retrieves unified FtjsResidentSearchResponse data for a specific resident by ID.
     */
    @Transactional(readOnly = true)
    public FtjsResidentSearchResponse getResidentFtjsProfile(Long residentId) {
        Resident resident = residentRepository.findById(residentId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found with id: " + residentId));
        return buildFtjsResponse(resident);
    }

    private FtjsResidentSearchResponse buildFtjsResponse(Resident resident) {
        Person person = resident.getPerson();

        String fullName = null;
        Integer age = null;
        String gender = null;
        String civilStatus = null;
        String contactNumber = null;
        String completeAddress = null;
        String residencyDate = null;

        if (person != null) {
            fullName = buildFullName(person.getFirstName(), person.getMiddleName(), person.getLastName(), person.getSuffix());
            if (person.getAge() != null) {
                age = (int) person.getAge();
            }
            gender = person.getGender();
            civilStatus = person.getCivilStatus();
            contactNumber = person.getContactNumber();
            completeAddress = person.getCompleteAddress();
        }

        if (resident.getDateOfResidency() != null) {
            residencyDate = resident.getDateOfResidency().toString();
        }

        // 1. Issuance History (and duplicate check flags)
        List<FirstTimeJobSeeker> ftjsList = ftjsRepository.findByResidentIdAndIsDeletedFalseOrderByCreatedAtDesc(resident.getResidentId());
        List<FtjsIssuanceHistoryResponse> issuanceHistories = ftjsList.stream().map(f -> new FtjsIssuanceHistoryResponse(
                f.getFtjsNumber(),
                f.getCreatedAt(),
                f.getStatus() != null ? f.getStatus().name() : null
        )).toList();

        boolean hasAvailedFtjs = !ftjsList.isEmpty();
        String priorTrackingNumber = null;
        String priorDateSubmitted = null;

        if (hasAvailedFtjs) {
            FirstTimeJobSeeker latest = ftjsList.get(0);
            priorTrackingNumber = latest.getFtjsNumber();
            priorDateSubmitted = latest.getDateSubmitted() != null ? latest.getDateSubmitted().toString() :
                    (latest.getCreatedAt() != null ? latest.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null);
        }

        // 2. Barangay Case History
        List<FtjsBarangayCaseHistoryResponse> caseHistoryList = List.of();
        if (person != null) {
            Long personId = person.getId();
            List<Case> cases = caseRepository.findByPersonIdInCaseRoles(personId);
            caseHistoryList = cases.stream().map(c -> {
                String role = "OTHER";
                if (c.getComplainant() != null && c.getComplainant().getPerson() != null && c.getComplainant().getPerson().getId().equals(personId)) {
                    role = "COMPLAINANT";
                } else if (c.getRespondent() != null && c.getRespondent().getPerson() != null && c.getRespondent().getPerson().getId().equals(personId)) {
                    role = "RESPONDENT";
                } else if (c.getWitnesses() != null && c.getWitnesses().stream().anyMatch(w -> w.getPerson() != null && w.getPerson().getId().equals(personId))) {
                    role = "WITNESS";
                }

                String incidentNature = c.getIncidentDetail() != null && c.getIncidentDetail().getNatureOfComplaint() != null ?
                        c.getIncidentDetail().getNatureOfComplaint() : "N/A";

                return new FtjsBarangayCaseHistoryResponse(
                        c.getCaseNumber() != null ? c.getCaseNumber() : "CASE-" + c.getId(),
                        incidentNature,
                        role,
                        c.getCreatedAt(),
                        c.getCaseStatus() != null ? c.getCaseStatus().name() : "PENDING"
                );
            }).toList();
        }

        return new FtjsResidentSearchResponse(
                resident.getResidentId(),
                fullName,
                age,
                gender,
                civilStatus,
                contactNumber,
                completeAddress,
                residencyDate,
                hasAvailedFtjs,
                priorTrackingNumber,
                priorDateSubmitted,
                issuanceHistories,
                caseHistoryList
        );
    }

    private String buildFullName(String first, String middle, String last, String suffix) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.isBlank()) sb.append(first.trim());
        if (middle != null && !middle.isBlank()) sb.append(" ").append(middle.trim());
        if (last != null && !last.isBlank()) sb.append(" ").append(last.trim());
        if (suffix != null && !suffix.isBlank()) sb.append(" ").append(suffix.trim());
        return sb.toString().trim();
    }
}
