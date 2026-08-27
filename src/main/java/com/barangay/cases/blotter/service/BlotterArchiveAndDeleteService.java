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
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class BlotterArchiveAndDeleteService {

    private final CaseRepository caseRepository;


    /**
     * Calculates the 4 lifecycle KPIs exclusively for Blotter cases: Total Archived,
     * Archived this month, Total in Trash, and Deleted this month.
     *
     * @return {@link BlotterLifecycleKpiResponse}
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
     *
     * @param filter {@link BlotterArchiveFilterRequest} search and filingType criteria
     * @return {@link PageResponse} of {@link BlotterArchiveItemResponse}
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
     * Excludes permanently deleted records.
     *
     * @param filter {@link BlotterArchiveFilterRequest} search and filingType criteria
     * @return {@link PageResponse} of {@link BlotterRecentDeleteItemResponse}
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

    /**
     * Moves a closed or completed case to the archive.
     *
     * @param caseId  the database identifier of the case
     * @param request payload containing archive remarks
     */
    @Transactional
    public void archiveCase(Long caseId, ArchiveCaseRequest request) {
        Case targetCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new NoSuchElementException("Case not found with ID: " + caseId));

        if (Boolean.TRUE.equals(targetCase.getIsDeleted())) {
            throw new IllegalStateException("Cannot archive a deleted case. Restore it from trash first.");
        }

        if (Boolean.TRUE.equals(targetCase.getIsArchived())) {
            throw new IllegalStateException("Case " + targetCase.getCaseNumber() + " is already archived.");
        }

        if (ACTIVE_STATUSES.contains(targetCase.getCaseStatus())) {
            throw new IllegalStateException(
                    "Cannot archive an ongoing case with status: " + targetCase.getCaseStatus() +
                            ". Settle, dismiss, or issue a Certificate to File Action first."
            );
        }
        targetCase.setIsArchived(true);
        if (request != null && request.remarks() != null && !request.remarks().isBlank()) {
            targetCase.setCaseStatusRemarks(request.remarks().trim());
        }

        caseRepository.save(targetCase);
    }

    /**
     * Restores an archived case back to active records.
     *
     * @param caseId the database identifier of the case
     */
    @Transactional
    public void restoreArchivedCase(Long caseId) {
        Case targetCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new NoSuchElementException("Case not found with ID: " + caseId));

        if (Boolean.TRUE.equals(targetCase.getIsDeleted())) {
            throw new IllegalStateException("Cannot restore a deleted case. Check recycle bin instead.");
        }

        if (Boolean.FALSE.equals(targetCase.getIsArchived()) || targetCase.getIsArchived() == null) {
            throw new IllegalStateException("Case " + targetCase.getCaseNumber() + " is not archived.");
        }

        targetCase.setIsArchived(false);
        targetCase.setArchiveRemarks(null);
        targetCase.setArchivedAt(null);
        caseRepository.save(targetCase);
    }
}
