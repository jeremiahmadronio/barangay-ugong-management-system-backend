package com.barangay.cases.bcpc.service;

import com.barangay.cases.blotter.dto.*;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.projection.BlotterLifecycleKpiProjection;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.TimelineEventType;
import com.barangay.zshared.mapper.PageResponse;
import com.barangay.zshared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class BcpcArchiveService {

    private final CaseRepository caseRepository;
    private final CaseTimelineService caseTimelineService;

    private static final java.util.Set<CaseStatus> ARCHIVABLE_STATUSES = java.util.Set.of(
            CaseStatus.SETTLED,
            CaseStatus.DISMISSED,
            CaseStatus.WITHDRAWN,
            CaseStatus.CLOSED
    );

    /**
     * Calculates the 4 lifecycle KPIs exclusively for BCPC cases:
     * Total Archived, Archived this month, Total in Trash, and Deleted this month.
     *
     * @return {@link BlotterLifecycleKpiResponse}
     */
    @Transactional(readOnly = true)
    public BlotterLifecycleKpiResponse getBcpcLifecycleKpi() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(LocalTime.MAX);

        BlotterLifecycleKpiProjection metrics = caseRepository.getBcpcLifecycleKpiMetrics(
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
     * Retrieves paginated archived BCPC cases for the Archive Table.
     *
     * @param filter {@link BlotterArchiveFilterRequest} search and filingType criteria
     * @return {@link PageResponse} of {@link BlotterArchiveItemResponse}
     */
    @Transactional(readOnly = true)
    public PageResponse<BlotterArchiveItemResponse> getArchivedBcpcCases(BlotterArchiveFilterRequest filter) {
        Pageable pageable = PageRequest.of(
                filter != null ? filter.getPageNumber() : 0,
                filter != null ? filter.getPageSize() : 10
        );
        Page<BlotterArchiveItemResponse> page = caseRepository.findArchivedBcpcCases(filter, pageable);
        return PageResponse.of(page);
    }

    /**
     * Retrieves paginated soft-deleted BCPC cases for the Recycle Bin / Trash Table.
     *
     * @param filter {@link BlotterArchiveFilterRequest} search and filingType criteria
     * @return {@link PageResponse} of {@link BlotterRecentDeleteItemResponse}
     */
    @Transactional(readOnly = true)
    public PageResponse<BlotterRecentDeleteItemResponse> getRecentDeletedBcpcCases(BlotterArchiveFilterRequest filter) {
        Pageable pageable = PageRequest.of(
                filter != null ? filter.getPageNumber() : 0,
                filter != null ? filter.getPageSize() : 10
        );
        Page<BlotterRecentDeleteItemResponse> page = caseRepository.findRecentDeletedBcpcCases(filter, pageable);
        return PageResponse.of(page);
    }

    private Case findRootCase(Case targetCase) {
        Case root = targetCase;
        while (root.getPreviousCaseNumber() != null && !root.getPreviousCaseNumber().trim().isEmpty()) {
            java.util.Optional<Case> prevOpt = caseRepository.findByCaseNumber(root.getPreviousCaseNumber().trim());
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
            java.util.Optional<Case> nextOpt = caseRepository.findByCaseNumber(curr.getReOpenedCaseNumber().trim());
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
     * Moves a BCPC case chain to the archive.
     *
     * @param caseId  the database identifier of any case in the chain
     * @param request payload containing archive remarks
     */
    @Transactional
    public void archiveCase(Long caseId, ArchiveCaseRequest request) {
        Case targetCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new NoSuchElementException("BCPC Case not found with ID: " + caseId));

        Case rootCase = findRootCase(targetCase);
        List<Case> lineage = getCaseLineage(rootCase);
        Case endmostChildCase = lineage.get(lineage.size() - 1);

        if (Boolean.TRUE.equals(endmostChildCase.getIsDeleted()) || Boolean.TRUE.equals(rootCase.getIsDeleted())) {
            throw new IllegalStateException("Cannot archive a deleted case. Restore it from trash first.");
        }

        if (Boolean.TRUE.equals(endmostChildCase.getIsArchived()) && Boolean.TRUE.equals(rootCase.getIsArchived())) {
            throw new IllegalStateException("BCPC Case " + rootCase.getCaseNumber() + " is already archived.");
        }

        CaseStatus currentStatus = endmostChildCase.getCaseStatus();
        if (currentStatus == null || !ARCHIVABLE_STATUSES.contains(currentStatus)) {
            throw new IllegalStateException(
                    "Cannot archive an active BCPC case. Case #" + endmostChildCase.getCaseNumber() +
                            " is currently " + (currentStatus != null ? currentStatus.name() : "PENDING") +
                            ". Only Settled, Dismissed, Withdrawn, or Closed cases can be archived."
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

        caseTimelineService.logEvent(
                rootCase,
                TimelineEventType.STATUS_CHANGED,
                "BCPC Case Archived",
                "Case was moved to Archive Vault with remarks: " + remarks,
                SecurityUtils.getCurrentUser()
        );

        log.info("BCPC Case #{} successfully archived with remarks: {}", rootCase.getCaseNumber(), remarks);
    }

    /**
     * Restores an archived or soft-deleted BCPC case chain back to active records.
     *
     * @param caseId the database identifier of any case in the chain
     */
    @Transactional
    public void restoreArchivedCase(Long caseId) {
        Case targetCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new NoSuchElementException("BCPC Case not found with ID: " + caseId));

        Case rootCase = findRootCase(targetCase);
        List<Case> lineage = getCaseLineage(rootCase);

        for (Case c : lineage) {
            c.setIsArchived(false);
            c.setArchiveRemarks(null);
            c.setArchivedAt(null);
            c.restoreDelete();
        }

        caseRepository.saveAll(lineage);

        caseTimelineService.logEvent(
                rootCase,
                TimelineEventType.STATUS_CHANGED,
                "BCPC Case Restored",
                "Case was restored from Archive/Trash back to active records.",
                SecurityUtils.getCurrentUser()
        );

        log.info("BCPC Case #{} successfully restored from archive/trash", rootCase.getCaseNumber());
    }

    /**
     * Moves a BCPC case chain to the recycle bin (soft delete).
     *
     * @param caseId the database identifier of any case in the chain
     */
    @Transactional
    public void softDelete(Long caseId) {
        Case targetCase = caseRepository.findById(caseId).orElseThrow(() ->
                new NoSuchElementException("BCPC Case not found with ID: " + caseId));

        Case rootCase = findRootCase(targetCase);
        List<Case> lineage = getCaseLineage(rootCase);

        boolean isAlreadyDeleted = lineage.stream()
                .anyMatch(c -> Boolean.TRUE.equals(c.getIsDeleted()));
        if (isAlreadyDeleted) {
            throw new IllegalStateException("Case chain for " + rootCase.getCaseNumber() + " is already in the trash.");
        }

        Case endmostChildCase = lineage.get(lineage.size() - 1);
        CaseStatus currentStatus = endmostChildCase.getCaseStatus();
        if (currentStatus == null || !ARCHIVABLE_STATUSES.contains(currentStatus)) {
            throw new IllegalStateException(
                    "Cannot move an active BCPC case to trash. Case #" + endmostChildCase.getCaseNumber() +
                            " is currently " + (currentStatus != null ? currentStatus.name() : "PENDING") +
                            ". Only Settled, Dismissed, Withdrawn, or Closed cases can be moved to trash."
            );
        }

        for (Case c : lineage) {
            c.softDelete();
        }
        caseRepository.saveAll(lineage);

        caseTimelineService.logEvent(
                rootCase,
                TimelineEventType.STATUS_CHANGED,
                "BCPC Case Moved to Trash",
                "Case was moved to the Recycle Bin (Soft Deleted).",
                SecurityUtils.getCurrentUser()
        );

        log.info("BCPC Case #{} moved to recycle bin", rootCase.getCaseNumber());
    }

    /**
     * Flags the BCPC case chain as permanently deleted (audit-safe purge).
     *
     * @param caseId the database identifier of any case in the chain
     */
    @Transactional
    public void permanentDeleteCaseByFlag(Long caseId) {
        Case targetCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new NoSuchElementException("BCPC Case not found with ID: " + caseId));

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

        caseTimelineService.logEvent(
                rootCase,
                TimelineEventType.STATUS_CHANGED,
                "BCPC Case Permanently Deleted",
                "Case was permanently deleted (purged from recycle bin).",
                SecurityUtils.getCurrentUser()
        );

        log.info("BCPC Case #{} permanently deleted", rootCase.getCaseNumber());
    }
}

