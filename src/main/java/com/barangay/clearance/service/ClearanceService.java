package com.barangay.clearance.service;

import com.barangay.clearance.dto.ClearanceStatsResponse;
import com.barangay.clearance.dto.IssuedCertificateHistoryItem;
import com.barangay.clearance.dto.IssuedCertificateTableResponse;
import com.barangay.clearance.model.IssuedCertificate;
import com.barangay.clearance.repository.CertificateTemplateRepository;
import com.barangay.clearance.repository.IssuedCertificateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
     * Excludes archived, soft-deleted, and permanently deleted certificates.
     *
     * @return {@link ClearanceStatsResponse}
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
     * For registered barangay residents (with person_id), multiple issuances are grouped
     * so only the most recent certificate is displayed in the main row, with all past
     * certificates included in its history timeline.
     * For non-residents/walk-ins, each transaction is displayed individually.
     *
     * @param search Search term matching certificate number, requestor name prefix, or person name prefix
     * @param typeOfCert Filter by certificate template title
     * @param purpose Filter by purpose
     * @return List of {@link IssuedCertificateTableResponse}
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
}
