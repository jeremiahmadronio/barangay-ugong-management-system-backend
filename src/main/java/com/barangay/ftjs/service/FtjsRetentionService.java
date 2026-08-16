package com.barangay.ftjs.service;

import com.barangay.ftjs.dto.RetentionSettingDto;
import com.barangay.ftjs.model.FtjsSystemSetting;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.ftjs.repository.FtjsSystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
/**
 * Service class managing data retention period settings and automated cleanup scheduled tasks.
 */
public class FtjsRetentionService {

    private final FtjsSystemSettingRepository ftjsSettingRepository;
    private final FirstTimeJobSeekerRepository ftjsRepository;

    private static final String RETENTION_KEY = "RETENTION_PERIOD";
    private static final String DEFAULT_RETENTION = "30";


    @Transactional(readOnly = true)
    /**
     * Fetches current system-wide retention period setting.
     */
    public RetentionSettingDto.Response getCurrentRetentionSetting() {
        String value = ftjsSettingRepository.findById(RETENTION_KEY)
                .map(FtjsSystemSetting::getSettingValue)
                .orElse(DEFAULT_RETENTION);
        return new RetentionSettingDto.Response(value);
    }

    @Transactional
    /**
     * Updates system-wide retention period setting.
     */
    public void updateRetentionSetting(RetentionSettingDto.Request request) {
        FtjsSystemSetting setting = ftjsSettingRepository.findById(RETENTION_KEY)
                .orElseGet(() -> new FtjsSystemSetting(RETENTION_KEY, request.retentionPeriod()));
        setting.setSettingValue(request.retentionPeriod().toUpperCase());
        ftjsSettingRepository.save(setting);
    }


    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    /**
     * Scheduled task executing periodic cleanup of soft-deleted records past retention threshold.
     */
    public void cleanupExpiredDeletedRecords() {
        String currentSetting = getCurrentRetentionSetting().retentionPeriod();

        if ("MANUAL".equalsIgnoreCase(currentSetting)) {
            return;
        }
        try {
            int days = Integer.parseInt(currentSetting);
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

            ftjsRepository.deleteChildAffidavitsExpired(cutoffDate);
            ftjsRepository.deleteChildTimelinesExpired(cutoffDate);
            ftjsRepository.deleteChildNotesExpired(cutoffDate);

            int deletedCount = ftjsRepository.hardDeleteExpiredRecords(cutoffDate);
            if (deletedCount > 0) {
                System.out.println("Auto-cleanup executed: " + deletedCount + " records permanently deleted (Threshold: " +  days + " days.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid retention period format in database: " + currentSetting);
        }
    }

}
