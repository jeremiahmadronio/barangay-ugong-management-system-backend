package com.barangay.cases.blotter.service;

import com.barangay.cases.blotter.dto.*;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.projection.BlotterLifecycleKpiProjection;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.mapper.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlotterArchiveAndDeleteService {

    private final CaseRepository caseRepository;


    /**
     * Calculates the 4 lifecycle KPIs exclusively for Blotter cases: Total Archived,.
     */
    @Transactional(readOnly = true)
    public BlotterLifecycleKpiResponse getBlotterLifecycleKpi() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);

        BlotterLifecycleKpiProjection metrics = caseRepository.getBlotterLifecycleKpiMetrics(
                startOfMonth,
                endOfMonth
        );

        long totalArchived = (metrics != null && metrics.getTotalArchived() != null)
                ? metrics.getTotalArchived().longValue() : 0L;
        long totalArchivedThisMonth = (metrics != null && metrics.getTotalArchivedThisMonth() != null)
                ? metrics.getTotalArchivedThisMonth().longValue() : 0L;
        long totalRecentlyDeleted = (metrics != null && metrics.getTotalRecentlyDeleted() != null)
                ? metrics.getTotalRecentlyDeleted().longValue() : 0L;
        long totalRecentlyDeletedThisMonth = (metrics != null && metrics.getTotalRecentlyDeletedThisMonth() != null)
                ? metrics.getTotalRecentlyDeletedThisMonth().longValue() : 0L;

        return new BlotterLifecycleKpiResponse(
                totalArchived,
                totalArchivedThisMonth,
                totalRecentlyDeleted,
                totalRecentlyDeletedThisMonth
        );
    }

    /**
     * Retrieves paginated archived blotter cases for the Archive Table.
     */
    @Transactional(readOnly = true)
    public PageResponse<BlotterArchiveItemResponse> getArchivedBlotterCases(BlotterArchiveFilterRequest filter) {
        Pageable pageable = PageRequest.of(
                filter != null ? filter.getPageNumber() : 0,
                filter != null ? filter.getPageSize() : 10
        );
        Page<BlotterArchiveItemResponse> page = caseRepository.findArchivedBlotterCases(filter, pageable);
        return PageResponse.of(page);
    }

    /**
     * Retrieves paginated soft-deleted blotter cases for the Recent Delete / Trash Table.
     */
    @Transactional(readOnly = true)
    public PageResponse<BlotterRecentDeleteItemResponse> getRecentDeletedBlotterCases(BlotterArchiveFilterRequest filter) {
        Pageable pageable = PageRequest.of(
                filter != null ? filter.getPageNumber() : 0,
                filter != null ? filter.getPageSize() : 10
        );
        Page<BlotterRecentDeleteItemResponse> page = caseRepository.findRecentDeletedBlotterCases(filter, pageable);
        return PageResponse.of(page);
    }



    private static final List<CaseStatus> ACTIVE_STATUSES = List.of(
            CaseStatus.PENDING,
            CaseStatus.UNDER_MEDIATION,
            CaseStatus.UNDER_CONCILIATION
    );

    private Case findRootCase(Case targetCase) {
        Case root = targetCase;
        while (root.getPreviousCaseNumber() != null && !root.getPreviousCaseNumber().trim().isEmpty()) {
            Optional<Case> prevOpt = caseRepository.findByCaseNumber(root.getPreviousCaseNumber().trim());
            if (prevOpt.isPresent()) {
                root = prevOpt.get();
            } else {
                break;
            }
        }
        return root;
    }

    private List<Case> getCaseLineage(Case rootCase) {
        List<Case> lineage = new ArrayList<>();
        Case curr = rootCase;
        lineage.add(curr);
        while (curr.getReOpenedCaseNumber() != null && !curr.getReOpenedCaseNumber().trim().isEmpty()) {
            Optional<Case> nextOpt = caseRepository.findByCaseNumber(curr.getReOpenedCaseNumber().trim());
            if (nextOpt.isPresent()) {
                curr = nextOpt.get();
                lineage.add(curr);
            } else {
                break;
            }
        }
        return lineage;
    }

    /**
     * Moves a closed or completed case chain to the archive.
     */
    @Transactional
    public void archiveCase(Long caseId, ArchiveCaseRequest request) {
        Case targetCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new NoSuchElementException("Case not found with ID: " + caseId));

        Case rootCase = findRootCase(targetCase);
        List<Case> lineage = getCaseLineage(rootCase);
        Case endmostChildCase = lineage.get(lineage.size() - 1);

        if (Boolean.TRUE.equals(endmostChildCase.getIsDeleted()) || Boolean.TRUE.equals(rootCase.getIsDeleted())) {
            throw new IllegalStateException("Cannot archive a deleted case. Restore it from trash first.");
        }

        if (Boolean.TRUE.equals(endmostChildCase.getIsArchived()) && Boolean.TRUE.equals(rootCase.getIsArchived())) {
            throw new IllegalStateException("Case " + rootCase.getCaseNumber() + " is already archived.");
        }

        if (ACTIVE_STATUSES.contains(endmostChildCase.getCaseStatus())) {
            throw new IllegalStateException(
                    "Cannot archive an ongoing case. Endmost active cycle (" + endmostChildCase.getCaseNumber() +
                            ") is still: " + endmostChildCase.getCaseStatus() +
                            ". Settle, dismiss, or issue a Certificate to File Action first."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        String remarks = (request != null && request.remarks() != null && !request.remarks().isBlank())
                ? request.remarks().trim()
                : "Archived Record";

        for (Case c : lineage) {
            c.setIsArchived(true);
            c.setArchivedAt(now);
            c.setArchiveRemarks(remarks);
        }

        caseRepository.saveAll(lineage);
    }

    /**
     * Restores an archived case chain (root case and all child cases) back to active records.
     */
    @Transactional
    public void restoreArchivedCase(Long caseId) {
        Case targetCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new NoSuchElementException("Case not found with ID: " + caseId));

        Case rootCase = findRootCase(targetCase);
        List<Case> lineage = getCaseLineage(rootCase);

        for (Case c : lineage) {
            c.setIsArchived(false);
            c.setIsDeleted(false);
            c.setArchiveRemarks(null);
            c.setArchivedAt(null);
            c.setDeletedAt(null);
        }

        caseRepository.saveAll(lineage);
    }


    /**
     * Moves a case chain to the recycle bin (soft delete).
     */
    @Transactional
    public void softDelete(Long caseId) {
        Case targetCase = caseRepository.findById(caseId).orElseThrow(() ->
                new NoSuchElementException("Case not found with ID: " + caseId));


        Case rootCase = findRootCase(targetCase);
        List<Case> lineage = getCaseLineage(rootCase);

        boolean isAlreadyDeleted = lineage.stream()
                .anyMatch(c -> Boolean.TRUE.equals(c.getIsDeleted()));
        if (isAlreadyDeleted) {
            throw new IllegalStateException("Case chain for " + rootCase.getCaseNumber() + " is already in the trash.");
        }

        Case endmostChildCase = lineage.get(lineage.size() - 1);
        if (ACTIVE_STATUSES.contains(endmostChildCase.getCaseStatus())) {
            throw new IllegalStateException(
                    "Cannot move an ongoing case to trash. Endmost active cycle (" + endmostChildCase.getCaseNumber() +
                            ") is currently: " + endmostChildCase.getCaseStatus() +
                            ". Settle, dismiss, or withdraw the case first."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        for (Case c : lineage) {
            c.setIsDeleted(true);
            c.setDeletedAt(now);
        }
        caseRepository.saveAll(lineage);
    }

    /**
     * Flags the case chain as permanently deleted (audit-safe purge).
     */
    @Transactional
    public void permanentDeleteCaseByFlag(Long caseId) {
        Case targetCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new NoSuchElementException("Case not found with ID: " + caseId));

        Case rootCase = findRootCase(targetCase);
        List<Case> lineage = getCaseLineage(rootCase);

        boolean isInTrash = lineage.stream()
                .anyMatch(c -> Boolean.TRUE.equals(c.getIsDeleted()));

        if (!isInTrash) {
            throw new IllegalStateException("Case must be in trash before it can be permanently deleted.");
        }

        for (Case c : lineage) {
            c.permanentDelete();
        }

        caseRepository.saveAll(lineage);
    }


}
