package com.barangay.ftjs.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.ftjs.dto.FtjsBarangayCaseHistoryResponse;
import com.barangay.ftjs.dto.FtjsIssuanceHistoryResponse;
import com.barangay.ftjs.dto.FtjsRequestEntry;
import com.barangay.ftjs.dto.FtjsResidentSearchResponse;
import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.ftjs.model.FirstTimeJobSeekerTimeLine;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerTimeLineRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.enumerated.FtjsStatus;
import com.barangay.zshared.enumerated.TimeLineType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.security.SecurityUtils;

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
    private final FirstTimeJobSeekerTimeLineRepository timeLineRepository;
    private final ResidentRepository residentRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;

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
                f.getCreatedAt() != null ? f.getCreatedAt() : (f.getDateSubmitted() != null ? f.getDateSubmitted().atStartOfDay() : LocalDateTime.now()),
                f.getStatus() != null ? f.getStatus().name() : "ISSUED"
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

    /**
     * Creates and saves a new FTJS entry into the database with attached binary files.
     */
    @Transactional
    public FtjsResidentSearchResponse createFtjsEntry(FtjsRequestEntry request) {
        if (request.residentId() == null) {
            throw new IllegalArgumentException("Resident ID is required");
        }

        Resident resident = residentRepository.findById(request.residentId())
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found with id: " + request.residentId()));

        Person person = resident.getPerson();

        FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
        ftjs.setResident(resident);
        ftjs.setPerson(person);
        ftjs.setFtjsNumber(request.ftjsNumber());
        ftjs.setPurposeDocuments(request.purpose());
        ftjs.setEducationalAttainment(request.educationalAttainment());
        ftjs.setCourse(request.gradeLevel());
        ftjs.setSchoolInstitution(request.institution());
        ftjs.setValidIdFiles(request.validId());
        ftjs.setOathFiles(request.oathTaking());

        if (request.dateFiled() != null) {
            ftjs.setDateSubmitted(request.dateFiled().toLocalDate());
        } else {
            ftjs.setDateSubmitted(LocalDate.now());
        }

        // Determine verifiedBy User entity (check authenticated user first, then username/email/name lookup)
        User verifiedUser = SecurityUtils.getCurrentUser();
        if (verifiedUser == null && request.releasedBy() != null && !request.releasedBy().isBlank()) {
            String releasedByStr = request.releasedBy().trim();
            verifiedUser = userRepository.findByUsername(releasedByStr)
                    .or(() -> userRepository.findBySystemEmail(releasedByStr))
                    .orElseGet(() -> userRepository.findAll().stream()
                            .filter(u -> (u.getPerson() != null && releasedByStr.equalsIgnoreCase(
                                    buildFullName(u.getPerson().getFirstName(), u.getPerson().getMiddleName(),
                                            u.getPerson().getLastName(), u.getPerson().getSuffix()))) ||
                                    releasedByStr.equalsIgnoreCase(u.getUsername()) ||
                                    releasedByStr.equalsIgnoreCase(u.getSystemEmail()))
                            .findFirst()
                            .orElse(null));
        }
        if (verifiedUser != null) {
            ftjs.setVerifiedBy(verifiedUser);
        }

        // Count existing non-deleted FTJS records for issuance count & status determination
        List<FirstTimeJobSeeker> existingRecords = ftjsRepository.findByResidentIdAndIsDeletedFalseOrderByCreatedAtDesc(resident.getResidentId());
        boolean isFirstIssuance = existingRecords.isEmpty();
        if (isFirstIssuance) {
            ftjs.setIssuanceCount(1);
            ftjs.setStatus(FtjsStatus.ISSUED);
        } else {
            ftjs.setIssuanceCount(existingRecords.size() + 1);
            ftjs.setStatus(FtjsStatus.RE_ISSUANCE);
        }

        LocalDateTime now = LocalDateTime.now();
        if (ftjs.getCreatedAt() == null) {
            ftjs.setCreatedAt(now);
        }

        // Persist FTJS record and flush so it gets a DB-assigned ID
        FirstTimeJobSeeker savedFtjs = ftjsRepository.saveAndFlush(ftjs);

        // ─── Save FTJS Timeline entry ──────────────────────────────────────────
        // This records the creation event in first_time_job_seeker_timeline table
        FirstTimeJobSeekerTimeLine timeline = new FirstTimeJobSeekerTimeLine();
        timeline.setFtjs(savedFtjs);
        timeline.setTimelineType(isFirstIssuance ? TimeLineType.SUBMITTED : TimeLineType.RE_ISSUANCE);
        timeline.setTitle(isFirstIssuance ? "FTJS Certificate Issued" : "FTJS Re-Issuance");
        timeline.setDescription(String.format(
                "%s processed for %s. Application No: %s. Educational Attainment: %s.",
                isFirstIssuance ? "First-time FTJS certificate" : "FTJS re-issuance",
                person != null ? buildFullName(person.getFirstName(), person.getMiddleName(),
                        person.getLastName(), person.getSuffix()) : "Resident",
                savedFtjs.getFtjsNumber(),
                request.educationalAttainment() != null ? request.educationalAttainment() : "N/A"
        ));
        timeline.setEventDate(now);
        timeline.setCreatedBy(verifiedUser);
        timeLineRepository.save(timeline);
        // ──────────────────────────────────────────────────────────────────────

        log.info("[FTJS] Entry saved — Resident ID: {}, FTJS No: {}, Timeline: {}",
                resident.getResidentId(), savedFtjs.getFtjsNumber(),
                isFirstIssuance ? "SUBMITTED" : "RE_ISSUANCE");

        return buildFtjsResponse(resident);
    }
}
