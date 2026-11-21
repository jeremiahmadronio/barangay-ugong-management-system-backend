package com.barangay.clearance.service;

import com.barangay.clearance.dto.ClearanceStatsResponse;
import com.barangay.clearance.dto.IssuedCertificateDetailResponse;
import com.barangay.clearance.dto.IssuedCertificateDetailResponse.ClearanceApplicantSummary;
import com.barangay.clearance.dto.IssuedCertificateDetailResponse.ClearanceOfficerSummary;
import com.barangay.clearance.dto.IssuedCertificateHistoryItem;
import com.barangay.clearance.dto.IssuedCertificateTableResponse;
import com.barangay.clearance.model.IssuedCertificate;
import com.barangay.clearance.repository.CertificateTemplateRepository;
import com.barangay.clearance.repository.IssuedCertificateRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.resident.model.Resident;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service managing clearance operations and dashboard KPI metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClearanceService {

    private final CertificateTemplateRepository certificateTemplateRepository;
    private final IssuedCertificateRepository issuedCertificateRepository;

    /**
     * Computes dashboard KPI statistics for clearance and certificate issuance.
     */
    @Transactional(readOnly = true)
    public ClearanceStatsResponse getClearanceStats() {
        long totalIssued = issuedCertificateRepository.countActiveCertificates();

        LocalDate now = LocalDate.now();
        LocalDateTime startOfMonth = now.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59);
        long issuedThisMonth = issuedCertificateRepository.countActiveCertificatesBetween(startOfMonth, endOfMonth);

        List<IssuedCertificate> allActive = issuedCertificateRepository.findAllActiveCertificates();

        // 1. Most Issued Certificate Template
        Map<String, Long> certCounts = new HashMap<>();
        for (IssuedCertificate c : allActive) {
            String title = (c.getTemplate() != null && c.getTemplate().getCertTitle() != null)
                    ? c.getTemplate().getCertTitle()
                    : "Barangay Clearance";
            certCounts.merge(title, 1L, Long::sum);
        }

        String mostIssuedCert = certCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Barangay Clearance");

        // 2. Most Common Purpose (extracted from fieldValues -> 'purpose' or remarks)
        Map<String, Long> purposeCounts = new HashMap<>();
        for (IssuedCertificate ic : allActive) {
            String purpose = extractPurpose(ic);
            if (purpose != null && !purpose.trim().isEmpty()) {
                purposeCounts.merge(purpose.trim(), 1L, Long::sum);
            }
        }

        String mostCommonPurpose = purposeCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Local Employment");

        return new ClearanceStatsResponse(
                totalIssued,
                issuedThisMonth,
                mostIssuedCert,
                mostCommonPurpose
        );
    }

    /**
     * Retrieves filtered active issued certificates for the management table.
     */
    @Transactional(readOnly = true)
    public List<IssuedCertificateTableResponse> getIssuedCertificates(
            String search,
            String typeOfCert,
            String purpose
    ) {
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String trimmedType = (typeOfCert != null && !typeOfCert.trim().isEmpty() && !typeOfCert.equalsIgnoreCase("ALL"))
                ? typeOfCert.trim()
                : null;
        String trimmedPurpose = (purpose != null && !purpose.trim().isEmpty() && !purpose.equalsIgnoreCase("ALL"))
                ? purpose.trim()
                : null;

        List<IssuedCertificate> certificates = issuedCertificateRepository.findActiveIssuedCertificates(
                trimmedSearch,
                trimmedType
        );

        // Map of resident personId -> list of all issued certificates for that resident (newest first)
        Map<Long, List<IssuedCertificateHistoryItem>> residentHistoryMap = new LinkedHashMap<>();
        Map<Long, IssuedCertificate> residentLatestCertMap = new LinkedHashMap<>();
        List<IssuedCertificate> nonResidentCerts = new ArrayList<>();

        for (IssuedCertificate ic : certificates) {
            IssuedCertificateHistoryItem historyItem = toHistoryItem(ic);

            if (ic.getPerson() != null && ic.getPerson().getId() != null) {
                Long personId = ic.getPerson().getId();
                residentHistoryMap.computeIfAbsent(personId, k -> new ArrayList<>()).add(historyItem);
                if (!residentLatestCertMap.containsKey(personId)) {
                    residentLatestCertMap.put(personId, ic);
                }
            } else {
                nonResidentCerts.add(ic);
            }
        }

        List<IssuedCertificateTableResponse> result = new ArrayList<>();

        // 1. Add grouped resident rows (showing latest certificate + full history)
        for (Map.Entry<Long, IssuedCertificate> entry : residentLatestCertMap.entrySet()) {
            Long personId = entry.getKey();
            IssuedCertificate latest = entry.getValue();
            List<IssuedCertificateHistoryItem> history = residentHistoryMap.getOrDefault(personId, List.of());

            result.add(new IssuedCertificateTableResponse(
                    latest.getId(),
                    latest.getCertNumber(),
                    latest.getTemplate() != null ? latest.getTemplate().getId() : null,
                    latest.getTemplate() != null ? latest.getTemplate().getCertTitle() : "BARANGAY CLEARANCE",
                    personId,
                    formatPersonName(latest),
                    latest.getOrNumber(),
                    latest.getCtnNumber(),
                    extractPurpose(latest),
                    latest.getStatus(),
                    latest.getExpiryDate(),
                    latest.getCreatedAt(),
                    latest.getFieldValues(),
                    history.size(),
                    history
            ));
        }

        // 2. Add non-resident / manual entries
        for (IssuedCertificate walkIn : nonResidentCerts) {
            IssuedCertificateHistoryItem item = toHistoryItem(walkIn);
            result.add(new IssuedCertificateTableResponse(
                    walkIn.getId(),
                    walkIn.getCertNumber(),
                    walkIn.getTemplate() != null ? walkIn.getTemplate().getId() : null,
                    walkIn.getTemplate() != null ? walkIn.getTemplate().getCertTitle() : "BARANGAY CLEARANCE",
                    null,
                    formatPersonName(walkIn),
                    walkIn.getOrNumber(),
                    walkIn.getCtnNumber(),
                    extractPurpose(walkIn),
                    walkIn.getStatus(),
                    walkIn.getExpiryDate(),
                    walkIn.getCreatedAt(),
                    walkIn.getFieldValues(),
                    1,
                    List.of(item)
            ));
        }

        // Sort overall table results by newest createdAt descending
        result.sort((a, b) -> {
            if (a.createdAt() == null || b.createdAt() == null) return 0;
            return b.createdAt().compareTo(a.createdAt());
        });

        // Filter by purpose if specified
        if (trimmedPurpose != null) {
            result = result.stream()
                    .filter(resp -> resp.purpose() != null && resp.purpose().toLowerCase().contains(trimmedPurpose.toLowerCase()))
                    .toList();
        }

        return result;
    }

    private IssuedCertificateHistoryItem toHistoryItem(IssuedCertificate ic) {
        String officerName = "Barangay Staff";
        if (ic.getIssuedBy() != null) {
            if (ic.getIssuedBy().getPerson() != null) {
                officerName = formatPersonDirect(ic.getIssuedBy().getPerson());
            } else if (ic.getIssuedBy().getUsername() != null) {
                officerName = ic.getIssuedBy().getUsername();
            }
        }

        return new IssuedCertificateHistoryItem(
                ic.getId(),
                ic.getCertNumber(),
                ic.getTemplate() != null ? ic.getTemplate().getId() : null,
                ic.getTemplate() != null ? ic.getTemplate().getCertTitle() : "BARANGAY CLEARANCE",
                ic.getOrNumber(),
                ic.getCtnNumber(),
                extractPurpose(ic),
                ic.getStatus(),
                ic.getExpiryDate(),
                ic.getCreatedAt(),
                officerName,
                ic.getFieldValues()
        );
    }

    private String extractPurpose(IssuedCertificate ic) {
        String purpose = null;
        JsonNode fields = ic.getFieldValues();
        if (fields != null && fields.hasNonNull("purpose")) {
            purpose = fields.get("purpose").asText();
        }
        if ((purpose == null || purpose.trim().isEmpty() || purpose.equalsIgnoreCase("Others") || purpose.equalsIgnoreCase("OTHER"))
                && fields != null && fields.hasNonNull("customPurpose")) {
            purpose = fields.get("customPurpose").asText();
        }
        if ((purpose == null || purpose.trim().isEmpty()) && ic.getArchiveRemarks() != null) {
            purpose = ic.getArchiveRemarks();
        }
        if (purpose == null || purpose.trim().isEmpty()) {
            return "General Purpose";
        }
        return purpose.trim();
    }

    private String formatPersonName(IssuedCertificate ic) {
        if (ic.getPerson() != null) {
            String last = ic.getPerson().getLastName();
            String first = ic.getPerson().getFirstName();
            String middle = ic.getPerson().getMiddleName();
            String suffix = ic.getPerson().getSuffix();

            if (last != null && !last.trim().isEmpty() && first != null && !first.trim().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append(last.trim()).append(", ").append(first.trim());
                if (middle != null && !middle.trim().isEmpty()) {
                    sb.append(" ").append(Character.toUpperCase(middle.trim().charAt(0))).append(".");
                }
                if (suffix != null && !suffix.trim().isEmpty()) {
                    sb.append(" ").append(suffix.trim());
                }
                return sb.toString();
            }
        }

        // Fallback: format raw requestorName string e.g. "Jeremiah Dragon Madronio" -> "Madronio, Jeremiah D."
        String raw = ic.getRequestorName();
        if (raw == null || raw.trim().isEmpty()) {
            return "Unknown Requestor";
        }
        raw = raw.trim();
        if (raw.contains(",")) {
            return raw;
        }
        String[] parts = raw.split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        if (parts.length == 2) {
            return parts[1] + ", " + parts[0];
        }
        if (parts.length >= 3) {
            String last = parts[parts.length - 1];
            String first = parts[0];
            String middle = parts[1];
            return last + ", " + first + " " + Character.toUpperCase(middle.charAt(0)) + ".";
        }
        return raw;
    }

    /**
     * Retrieves full details of an issued certificate by ID.
     */
    @Transactional(readOnly = true)
    public IssuedCertificateDetailResponse getIssuedCertificateDetail(Long id) {
        IssuedCertificate ic = issuedCertificateRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Issued Certificate with ID " + id + " not found."
                ));

        // 1. Build Issuing Officer Summary
        ClearanceOfficerSummary officerSummary = null;
        if (ic.getIssuedBy() != null) {
            User u = ic.getIssuedBy();
            String officerName = null;
            if (u.getPerson() != null) {
                officerName = formatPersonDirect(u.getPerson());
            }
            if (officerName == null || officerName.isBlank()) {
                officerName = u.getUsername();
            }

            String roleName = u.getRole() != null ? u.getRole().getRoleName() : "Officer";
            String empId = (u.getPerson() != null && u.getPerson().getResident() != null)
                    ? u.getPerson().getResident().getBarangayIdNumber()
                    : null;

            officerSummary = new ClearanceOfficerSummary(
                    u.getId(),
                    officerName,
                    u.getUsername(),
                    roleName,
                    empId
            );
        }

        // 2. Build Applicant Demographics Summary
        ClearanceApplicantSummary applicantSummary;
        if (ic.getPerson() != null) {
            Person p = ic.getPerson();
            Resident r = p.getResident();

            String fullName = formatPersonDirect(p);
            Integer age = p.getAge() != null ? p.getAge().intValue() : null;
            String brgyId = r != null ? r.getBarangayIdNumber() : null;
            Long residentId = r != null ? r.getResidentId() : null;
            String citizenship = r != null ? r.getCitizenship() : "Filipino";

            applicantSummary = new ClearanceApplicantSummary(
                    p.getId(),
                    residentId,
                    p.getFirstName(),
                    p.getMiddleName(),
                    p.getLastName(),
                    p.getSuffix(),
                    fullName,
                    age,
                    p.getGender(),
                    p.getCivilStatus(),
                    p.getContactNumber(),
                    p.getEmail(),
                    p.getCompleteAddress(),
                    p.getBirthDate(),
                    null,
                    citizenship,
                    p.getOccupation(),
                    brgyId,
                    Boolean.TRUE.equals(p.getIsResident())
            );
        } else {
            // Walk-in / non-resident without person entity link
            JsonNode fields = ic.getFieldValues();
            String contact = (fields != null && fields.hasNonNull("contactNumber")) ? fields.get("contactNumber").asText() : null;
            String address = (fields != null && fields.hasNonNull("address")) ? fields.get("address").asText() : null;
            String gender = (fields != null && fields.hasNonNull("gender")) ? fields.get("gender").asText() : null;
            String civilStatus = (fields != null && fields.hasNonNull("civilStatus")) ? fields.get("civilStatus").asText() : null;
            String occupation = (fields != null && fields.hasNonNull("occupation")) ? fields.get("occupation").asText() : null;

            applicantSummary = new ClearanceApplicantSummary(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    ic.getRequestorName() != null ? ic.getRequestorName().trim() : "Unknown Requestor",
                    null,
                    gender,
                    civilStatus,
                    contact,
                    null,
                    address,
                    null,
                    null,
                    "Filipino",
                    occupation,
                    null,
                    false
            );
        }

        // 3. Build Past Issuance History (excluding currently viewed certificate)
        List<IssuedCertificateHistoryItem> history = new ArrayList<>();
        List<IssuedCertificate> pastCerts = new ArrayList<>();
        if (ic.getPerson() != null && ic.getPerson().getId() != null) {
            pastCerts = issuedCertificateRepository.findActiveCertificatesByPersonId(ic.getPerson().getId());
        } else if (ic.getRequestorName() != null && !ic.getRequestorName().isBlank()) {
            pastCerts = issuedCertificateRepository.findActiveCertificatesByPersonOrName(null, ic.getRequestorName().trim());
        }

        for (IssuedCertificate past : pastCerts) {
            if (!past.getId().equals(ic.getId())) {
                history.add(toHistoryItem(past));
            }
        }

        String templateTitle = ic.getTemplate() != null ? ic.getTemplate().getCertTitle() : "Barangay Clearance";
        Long templateId = ic.getTemplate() != null ? ic.getTemplate().getId() : null;

        return new IssuedCertificateDetailResponse(
                ic.getId(),
                ic.getCertNumber(),
                templateId,
                templateTitle,
                templateTitle,
                extractPurpose(ic),
                ic.getOrNumber(),
                ic.getCtnNumber(),
                ic.getAmountPaid() != null ? ic.getAmountPaid() : BigDecimal.ZERO,
                ic.isFree(),
                ic.getSignatoryName(),
                ic.getSignatoryPosition(),
                ic.getStatus() != null ? ic.getStatus().name() : "ACTIVE",
                ic.getExpiryDate(),
                ic.getCreatedAt(),
                ic.getUpdatedAt(),
                ic.getFieldValues(),
                ic.getIsArchived(),
                ic.getArchivedAt(),
                ic.getArchiveRemarks(),
                officerSummary,
                applicantSummary,
                history.size(),
                history
        );
    }

    private String formatPersonDirect(Person person) {
        if (person == null) return null;
        StringBuilder sb = new StringBuilder();
        if (person.getFirstName() != null && !person.getFirstName().isBlank()) {
            sb.append(person.getFirstName().trim());
        }
        if (person.getMiddleName() != null && !person.getMiddleName().isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(person.getMiddleName().trim());
        }
        if (person.getLastName() != null && !person.getLastName().isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(person.getLastName().trim());
        }
        if (person.getSuffix() != null && !person.getSuffix().isBlank()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(person.getSuffix().trim());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
