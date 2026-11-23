package com.barangay.zshared.scheduler;

import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.zshared.entity.ArchiveSettings;
import com.barangay.zshared.enumerated.OfficerStatus;
import com.barangay.zshared.repository.ArchiveSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job for officer archive retention and automated permanent deletion.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OfficerScheduler {

    private final ArchiveSettingsRepository archiveSettingsRepository;
    private final OfficerRepository officerRepository;

    /**
     * Executes daily at midnight to permanently delete soft-deleted officer records exceeding retention threshold.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void autoPurgeExpiredSoftDeletedOfficers() {
        log.info("[OfficerScheduler] Checking soft-deleted officers exceeding retention threshold for permanent deletion...");
        try {
            ArchiveSettings settings = archiveSettingsRepository.findById(1L).orElse(null);
            if (settings == null || !Boolean.TRUE.equals(settings.getAutoDeleteEnabled())) {
                log.info("[OfficerScheduler] Auto-delete retention is currently disabled.");
                return;
            }

            int retentionDays = settings.getDeleteAfterDays() != null ? settings.getDeleteAfterDays() : 30;
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

            List<Officer> expiredOfficers = officerRepository.findExpiredSoftDeletedOfficers(cutoff);
            if (expiredOfficers.isEmpty()) {
                log.info("[OfficerScheduler] No soft-deleted officers found exceeding {} days retention.", retentionDays);
                return;
            }

            int processedCount = 0;
            for (Officer o : expiredOfficers) {
                try {
                    o.permanentDelete();
                    o.setStatus(OfficerStatus.INACTIVE);
                    officerRepository.save(o);
                    processedCount++;
                    log.info("[OfficerScheduler] Auto-transferred expired soft-deleted officer to permanent delete: ID {}", o.getId());
                } catch (Exception e) {
                    log.warn("[OfficerScheduler] Failed to auto-permanent delete officer ID {}: {}", o.getId(), e.getMessage());
                }
            }

            log.info("[OfficerScheduler] Midnight officer trash retention check completed. Processed {} officer(s).", processedCount);
        } catch (Exception e) {
            log.error("[OfficerScheduler] Error running auto-transfer officer task: {}", e.getMessage(), e);
        }
    }
}
