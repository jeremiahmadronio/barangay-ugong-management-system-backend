package com.barangay.ftjs.service;

import com.barangay.ftjs.dto.FtjsArchiveStatsResponse;
import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.ftjs.model.FirstTimeJobSeekerTimeLine;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerTimeLineRepository;
import com.barangay.ftjs.repository.FtjsSystemSettingRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.audit.service.AuditLogService;
import com.barangay.zshared.enumerated.TimeLineType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.SecondaryRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class FtjsArchiveAndDeleteService {

    private final FirstTimeJobSeekerRepository ftjsRepository;
    private final FtjsSystemSettingRepository ftjsSettingRepository;
    private final FirstTimeJobSeekerTimeLineRepository ftjsTimeLineRepository;




    @Transactional
    public void archiveFtjs(Long id){

        FirstTimeJobSeeker firstTimeJobSeeker = ftjsRepository.findById(id).orElseThrow((
        ) -> new ResourceNotFoundException("Ftjs not found"));

        if(Boolean.TRUE.equals(firstTimeJobSeeker.getIsArchived())){
            throw new RuntimeException("Ftjs is already archived");
        }
        User currentUser = SecurityUtils.getCurrentUser();

        firstTimeJobSeeker.setIsArchived(true);
        firstTimeJobSeeker.setArchivedAt(LocalDateTime.now());
        firstTimeJobSeeker.setArchiveRemarks("Archive Ftjs Application");
        ftjsRepository.save(firstTimeJobSeeker);

        FirstTimeJobSeekerTimeLine timeline = new FirstTimeJobSeekerTimeLine();

        timeline.setFtjs(firstTimeJobSeeker);
        timeline.setTimelineType(TimeLineType.ARCHIVED);
        timeline.setTitle("Application Archived");
        timeline.setDescription("Application moved to Archive Vault.");
        timeline.setEventDate(LocalDateTime.now());
        timeline.setCreatedBy(currentUser);
        ftjsTimeLineRepository.save(timeline);

    }



    @Transactional
    public void restoreFtjs(Long id){

        FirstTimeJobSeeker firstTimeJobSeeker = ftjsRepository.findById(id).orElseThrow((
        ) -> new ResourceNotFoundException("Ftjs not found"));

        if(Boolean.FALSE.equals(firstTimeJobSeeker.getIsArchived())){
            throw new RuntimeException("Ftjs is already Active");
        }
        User currentUser = SecurityUtils.getCurrentUser();
        firstTimeJobSeeker.setIsArchived(false);
        ftjsRepository.save(firstTimeJobSeeker);

        FirstTimeJobSeekerTimeLine timeline = new FirstTimeJobSeekerTimeLine();

        timeline.setFtjs(firstTimeJobSeeker);
        timeline.setTimelineType(TimeLineType.RESTORED);
        timeline.setTitle("Application Restored");
        timeline.setDescription("Application moved to Ftjs Management.");
        timeline.setEventDate(LocalDateTime.now());
        timeline.setCreatedBy(currentUser);
        ftjsTimeLineRepository.save(timeline);

    }


    @Transactional(readOnly = true)
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

}
