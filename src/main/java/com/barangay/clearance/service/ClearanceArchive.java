package com.barangay.clearance.service;

import com.barangay.clearance.dto.ArchiveClearanceRequest;
import com.barangay.clearance.dto.ClearanceArchiveItemResponse;
import com.barangay.clearance.dto.ClearanceLifecycleKpiResponse;
import com.barangay.clearance.dto.ClearanceRecentDeleteItemResponse;
import com.barangay.clearance.model.IssuedCertificate;
import com.barangay.clearance.projection.ClearanceLifecycleKpiProjection;
import com.barangay.clearance.repository.IssuedCertificateRepository;
import com.barangay.people.person.model.Person;
import com.barangay.zshared.enumerated.ClearanceStatus;
import com.barangay.zshared.mapper.PageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.NoSuchElementException;

/**
 * Service managing clearance archive vault, trash lifecycle, and KPI statistics.
 */
@Service
@RequiredArgsConstructor
public class ClearanceArchive {

    private final IssuedCertificateRepository issuedCertificateRepository;

    /**
     * Calculates monthly lifecycle KPI metrics for clearance archive and trash vaults.
     */
    @Transactional(readOnly = true)
    public ClearanceLifecycleKpiResponse getClearanceLifecycleKpi() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).with(LocalTime.MIN);
        LocalDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);

        ClearanceLifecycleKpiProjection projection = issuedCertificateRepository.getClearanceLifecycleKpiMetrics(startOfMonth, endOfMonth);

        long totalArchived = 0L;
        long totalArchivedThisMonth = 0L;
        long totalRecentlyDeleted = 0L;
        long totalRecentlyDeletedThisMonth = 0L;

        if (projection != null) {
            totalArchived = projection.getTotalArchived() != null ? projection.getTotalArchived().longValue() : 0L;
            totalArchivedThisMonth = projection.getTotalArchivedThisMonth() != null ? projection.getTotalArchivedThisMonth().longValue() : 0L;
            totalRecentlyDeleted = projection.getTotalRecentlyDeleted() != null ? projection.getTotalRecentlyDeleted().longValue() : 0L;
            totalRecentlyDeletedThisMonth = projection.getTotalRecentlyDeletedThisMonth() != null ? projection.getTotalRecentlyDeletedThisMonth().longValue() : 0L;
        }

        return new ClearanceLifecycleKpiResponse(
                totalArchived,
                totalArchivedThisMonth,
                totalRecentlyDeleted,
                totalRecentlyDeletedThisMonth
        );
    }

    /**
     * Retrieves paginated archived clearance records.
     */
    @Transactional(readOnly = true)
    public PageResponse<ClearanceArchiveItemResponse> getArchivedCertificates(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<IssuedCertificate> entityPage = issuedCertificateRepository.findArchivedCertificates(trimmedSearch, pageable);
        Page<ClearanceArchiveItemResponse> dtoPage = entityPage.map(this::toArchiveItemResponse);

        return PageResponse.of(dtoPage);
    }

    /**
     * Retrieves paginated recently deleted clearance records in the trash vault.
     */
    @Transactional(readOnly = true)
    public PageResponse<ClearanceRecentDeleteItemResponse> getRecentDeletedCertificates(String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<IssuedCertificate> entityPage = issuedCertificateRepository.findRecentDeletedCertificates(trimmedSearch, pageable);
        Page<ClearanceRecentDeleteItemResponse> dtoPage = entityPage.map(this::toRecentDeleteItemResponse);

        return PageResponse.of(dtoPage);
    }

    /**
     * Moves an issued certificate to the archive vault.
     */
    @Transactional
    public void archiveCertificate(Long id, ArchiveClearanceRequest request) {
        IssuedCertificate ic = issuedCertificateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Issued Certificate not found with ID: " + id));

        if (Boolean.TRUE.equals(ic.getIsDeleted())) {
            throw new IllegalStateException("Cannot archive a deleted certificate. Restore it from trash first.");
        }

        if (Boolean.TRUE.equals(ic.getIsArchived())) {
            throw new IllegalStateException("Certificate " + ic.getCertNumber() + " is already archived.");
        }

        LocalDateTime now = LocalDateTime.now();
        String remarks = (request != null && request.remarks() != null && !request.remarks().isBlank())
                ? request.remarks().trim()
                : "Archived Record";

        ic.setIsArchived(true);
        ic.setArchivedAt(now);
        ic.setArchiveRemarks(remarks);

        issuedCertificateRepository.save(ic);
    }

    /**
     * Restores an archived certificate back to active records.
     */
    @Transactional
    public void restoreArchivedCertificate(Long id) {
        IssuedCertificate ic = issuedCertificateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Issued Certificate not found with ID: " + id));

        ic.setIsArchived(false);
        ic.setArchivedAt(null);
        ic.setArchiveRemarks(null);
        ic.setIsDeleted(false);
        ic.setDeletedAt(null);

        if (ic.getExpiryDate() == null || ic.getExpiryDate().isAfter(LocalDate.now())) {
            ic.setStatus(ClearanceStatus.ACTIVE);
        } else {
            ic.setStatus(ClearanceStatus.EXPIRED);
        }

        issuedCertificateRepository.save(ic);
    }

    /**
     * Soft deletes an issued certificate and moves it to trash.
     */
    @Transactional
    public void softDelete(Long id) {
        IssuedCertificate ic = issuedCertificateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Issued Certificate not found with ID: " + id));

        if (Boolean.TRUE.equals(ic.getIsDeleted())) {
            throw new IllegalStateException("Certificate " + ic.getCertNumber() + " is already in the trash.");
        }

        ic.setIsDeleted(true);
        ic.setDeletedAt(LocalDateTime.now());
        ic.setIsPermanentDeleted(false);

        issuedCertificateRepository.save(ic);
    }

    /**
     * Restores a soft-deleted certificate from trash back to active records.
     */
    @Transactional
    public void restoreDeletedCertificate(Long id) {
        IssuedCertificate ic = issuedCertificateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Issued Certificate not found with ID: " + id));

        ic.setIsDeleted(false);
        ic.setDeletedAt(null);
        ic.setIsArchived(false);
        ic.setArchivedAt(null);
        ic.setArchiveRemarks(null);
        ic.setIsPermanentDeleted(false);
        ic.setPermanentDeletedAt(null);

        if (ic.getExpiryDate() == null || ic.getExpiryDate().isAfter(LocalDate.now())) {
            ic.setStatus(ClearanceStatus.ACTIVE);
        } else {
            ic.setStatus(ClearanceStatus.EXPIRED);
        }

        issuedCertificateRepository.save(ic);
    }

    /**
     * Permanently purges a soft-deleted certificate from the trash vault.
     */
    @Transactional
    public void permanentDeleteCertificate(Long id) {
        IssuedCertificate ic = issuedCertificateRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Issued Certificate not found with ID: " + id));

        if (!Boolean.TRUE.equals(ic.getIsDeleted())) {
            throw new IllegalStateException("Certificate must be in trash before it can be permanently deleted.");
        }

        ic.setIsDeleted(false);
        ic.setIsPermanentDeleted(true);
        ic.setPermanentDeletedAt(LocalDateTime.now());

        issuedCertificateRepository.save(ic);
    }

    private ClearanceArchiveItemResponse toArchiveItemResponse(IssuedCertificate ic) {
        Long personId = ic.getPerson() != null ? ic.getPerson().getId() : null;
        String templateTitle = ic.getTemplate() != null ? ic.getTemplate().getCertTitle() : "BARANGAY CLEARANCE";
        Long templateId = ic.getTemplate() != null ? ic.getTemplate().getId() : null;

        return new ClearanceArchiveItemResponse(
                ic.getId(),
                ic.getCertNumber(),
                templateId,
                templateTitle,
                personId,
                formatPersonName(ic),
                ic.getOrNumber(),
                ic.getCtnNumber(),
                extractPurpose(ic),
                ic.getArchiveRemarks() != null && !ic.getArchiveRemarks().isBlank()
                        ? ic.getArchiveRemarks() : "Archived Record",
                ic.getStatus(),
                ic.getExpiryDate(),
                ic.getArchivedAt() != null ? ic.getArchivedAt() : ic.getUpdatedAt(),
                ic.getCreatedAt()
        );
    }

    private ClearanceRecentDeleteItemResponse toRecentDeleteItemResponse(IssuedCertificate ic) {
        Long personId = ic.getPerson() != null ? ic.getPerson().getId() : null;
        String templateTitle = ic.getTemplate() != null ? ic.getTemplate().getCertTitle() : "BARANGAY CLEARANCE";
        Long templateId = ic.getTemplate() != null ? ic.getTemplate().getId() : null;

        return new ClearanceRecentDeleteItemResponse(
                ic.getId(),
                ic.getCertNumber(),
                templateId,
                templateTitle,
                personId,
                formatPersonName(ic),
                ic.getOrNumber(),
                ic.getCtnNumber(),
                extractPurpose(ic),
                ic.getStatus(),
                ic.getExpiryDate(),
                ic.getDeletedAt() != null ? ic.getDeletedAt() : ic.getUpdatedAt(),
                ic.getCreatedAt()
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
            Person p = ic.getPerson();
            StringBuilder sb = new StringBuilder();
            if (p.getFirstName() != null && !p.getFirstName().isBlank()) {
                sb.append(p.getFirstName().trim());
            }
            if (p.getMiddleName() != null && !p.getMiddleName().isBlank()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(p.getMiddleName().trim().charAt(0)).append(".");
            }
            if (p.getLastName() != null && !p.getLastName().isBlank()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(p.getLastName().trim());
            }
            if (p.getSuffix() != null && !p.getSuffix().isBlank()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(p.getSuffix().trim());
            }
            if (!sb.isEmpty()) {
                return sb.toString();
            }
        }
        if (ic.getRequestorName() != null && !ic.getRequestorName().isBlank()) {
            return ic.getRequestorName().trim();
        }
        return "N/A";
    }
}