package com.barangay.cases.blotter.service;

import com.barangay.cases.blotter.dto.BlotterLifecycleKpiResponse;
import com.barangay.cases.shared.projection.BlotterLifecycleKpiProjection;
import com.barangay.cases.shared.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

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


}
