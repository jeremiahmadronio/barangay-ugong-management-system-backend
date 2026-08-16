package com.barangay.ftjs.service;

import com.barangay.ftjs.dto.FtjsArchiveStatsResponse;
import com.barangay.ftjs.dto.FtjsSummaryResponse;
import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.ftjs.model.FirstTimeJobSeekerTimeLine;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerTimeLineRepository;
import com.barangay.ftjs.repository.FtjsSystemSettingRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.audit.service.AuditLogService;
import com.barangay.zshared.enumerated.FtjsStatus;
import com.barangay.zshared.enumerated.TimeLineType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.mapper.PageResponse;
import com.barangay.zshared.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.SecondaryRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * Service class executing soft delete, archive, restore, and permanent deletion operations for FTJS records.
 */
public class FtjsArchiveAndDeleteService {

    private final FirstTimeJobSeekerRepository ftjsRepository;
    private final FtjsSystemSettingRepository ftjsSettingRepository;
    private final FirstTimeJobSeekerTimeLineRepository ftjsTimeLineRepository;




    @Transactional
    /**
     * Archives an active FTJS record.
     */
    public void archiveFtjs(Long id){

        int affectedRows = ftjsRepository.archiveRecordNative(id);
        if (affectedRows == 0) {
            throw new ResourceNotFoundException("Ftjs not found with ID: " + id);
        }

        User currentUser = SecurityUtils.getCurrentUser();
        FirstTimeJobSeekerTimeLine timeline = new FirstTimeJobSeekerTimeLine();
        FirstTimeJobSeeker ref = new FirstTimeJobSeeker();
        ref.setId(id);

        timeline.setFtjs(ref);
        timeline.setTimelineType(TimeLineType.ARCHIVED);
        timeline.setTitle("Application Archived");
        timeline.setDescription("Application moved to Archive Vault.");
        timeline.setEventDate(LocalDateTime.now());
        timeline.setCreatedBy(currentUser);
        ftjsTimeLineRepository.save(timeline);

    }



    @Transactional
    /**
     * Restores an archived or soft-deleted FTJS record back to active state.
     */
    public void restoreFtjs(Long id){

        int affectedRows = ftjsRepository.restoreRecordNative(id);
        if (affectedRows == 0) {
            throw new ResourceNotFoundException("Ftjs not found with ID: " + id);
        }

        User currentUser = SecurityUtils.getCurrentUser();
        FirstTimeJobSeekerTimeLine timeline = new FirstTimeJobSeekerTimeLine();
        FirstTimeJobSeeker ref = new FirstTimeJobSeeker();
        ref.setId(id);

        timeline.setFtjs(ref);
        timeline.setTimelineType(TimeLineType.RESTORED);
        timeline.setTitle("Application Restored");
        timeline.setDescription("Application moved to Ftjs Management.");
        timeline.setEventDate(LocalDateTime.now());
        timeline.setCreatedBy(currentUser);
        ftjsTimeLineRepository.save(timeline);

    }


    @Transactional(readOnly = true)
    /**
     * Computes metrics and statistics for archived and soft-deleted FTJS records.
     */
    public FtjsArchiveStatsResponse getArchiveStats() {
        YearMonth currentYearMonth = YearMonth.now();
        LocalDateTime startOfCurrentMonth = currentYearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfCurrentMonth = currentYearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        YearMonth lastYearMonth = currentYearMonth.minusMonths(1);
        LocalDateTime startOfLastMonth = lastYearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfLastMonth = lastYearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        long totalArchived = ftjsRepository.countByIsArchivedTrue();
        long archivedThisMonth = ftjsRepository.countArchiveBetween(startOfCurrentMonth, endOfCurrentMonth);
        long archivedLastMonth = ftjsRepository.countArchiveBetween(startOfLastMonth, endOfLastMonth);
        long totalOriginalArchived = ftjsRepository.countArchiveOriginalIssuance();
        long totalRecentlyDeleted = ftjsRepository.countTotalRecentDelete();

        String trend = calculateTrend(archivedThisMonth, archivedLastMonth);
        return new FtjsArchiveStatsResponse(
                totalArchived,
                archivedThisMonth,
                trend,
                totalOriginalArchived,
                totalRecentlyDeleted
        );
    }

    /**
     * Helper method para sa pag-calculate ng percentage trend
     */
    private String calculateTrend(long currentMonthCount, long lastMonthCount) {
        if (lastMonthCount == 0) {
            if (currentMonthCount == 0) {
                return "0.0% vs last month";
            }
            return "+100.0% vs last month";
        }
        double percentage = ((double) (currentMonthCount - lastMonthCount) / lastMonthCount) * 100.0;
        String sign = percentage > 0 ? "+" : "";

        return String.format("%s%.1f%% vs last month", sign, percentage);
    }



    @Transactional(readOnly = true)
    public Page<FtjsSummaryResponse> getArchivedSummaryTable(
            String search, FtjsStatus status, String purpose, Integer issuanceCount, Pageable pageable) {
        return ftjsRepository.findArchiveSummaryFiltered(search, status, purpose, issuanceCount, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponse<FtjsSummaryResponse> getDeletedSummaryTable(
            String search, FtjsStatus status, String purpose, Integer issuanceCount, Pageable pageable) {

        String sanitizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String statusStr = status != null ? status.name() : null;

        Page<FirstTimeJobSeeker> deletedPage = ftjsRepository.findDeletedFilteredNative(
                sanitizedSearch, statusStr, purpose, issuanceCount, pageable);

        Page<FtjsSummaryResponse> dtoPage = deletedPage.map(f -> new FtjsSummaryResponse(
                f.getId(),
                f.getFtjsNumber(),
                f.getPerson() != null
                        ? (f.getPerson().getFirstName() + " " + (f.getPerson().getMiddleName() != null ? f.getPerson().getMiddleName() + " " : "") + f.getPerson().getLastName()).trim()
                        : "Resident",
                f.getIssuanceCount() != null ? f.getIssuanceCount().longValue() : 1L,
                f.getStatus(),
                f.getPurposeDocuments(),
                f.getDeletedAt() != null ? f.getDeletedAt() : f.getCreatedAt()
        ));

        return PageResponse.of(dtoPage);
    }



    @Transactional
    /**
     * Permanently deletes an FTJS record and all associated child entities.
     */
    public void permanentlyDelete(Long id) {
        ftjsRepository.deleteChildAffidavits(id);
        ftjsRepository.deleteChildTimelines(id);
        ftjsRepository.deleteChildNotes(id);

        int affectedRows = ftjsRepository.hardDeleteById(id);
        if (affectedRows == 0) {
            throw new EntityNotFoundException("Deleted FTJS application with ID " + id + " not found in trash.");
        }
    }


    @Transactional
    public void permanentlyDeleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ftjsRepository.deleteChildAffidavitsBatch(ids);
        ftjsRepository.deleteChildTimelinesBatch(ids);
        ftjsRepository.deleteChildNotesBatch(ids);
        ftjsRepository.hardDeleteByIds(ids);
    }


    @Transactional
    /**
     * Performs a soft delete on an FTJS record.
     */
    public void softDelete(Long id) {
        FirstTimeJobSeeker ftjs = ftjsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Active FTJS record with ID " + id + " not found."));

        ftjs.setDeleted(true);
        ftjs.setDeletedAt(LocalDateTime.now());
        ftjsRepository.save(ftjs);

        User actor = SecurityUtils.getCurrentUser();

        FirstTimeJobSeekerTimeLine timeline = new FirstTimeJobSeekerTimeLine();
        timeline.setFtjs(ftjs);
        timeline.setTimelineType(TimeLineType.SOFT_DELETED);
        timeline.setTitle("Application Moved to Trash");
        timeline.setDescription(String.format("Application record (%s) was soft-deleted and moved to Recently Deleted.", ftjs.getFtjsNumber()));
        timeline.setEventDate(LocalDateTime.now());
        timeline.setCreatedBy(actor);
        ftjsTimeLineRepository.save(timeline);
    }


}
