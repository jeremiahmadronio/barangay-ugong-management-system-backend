package com.barangay.zshared.scheduler;

import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.zshared.entity.ArchiveSettings;
import com.barangay.zshared.enumerated.ResidentStatus;
import com.barangay.zshared.repository.ArchiveSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job for Resident Record management:
 * Executes daily at 12:00 AM (midnight) to automatically transition expired
 * soft-deleted resident records in the trash vault into permanent-delete status
 * based on Barangay Archive Settings retention days.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResidentScheduler {

    private final ArchiveSettingsRepository archiveSettingsRepository;
    private final ResidentRepository residentRepository;

    @Scheduled(cron = "0 0 0 * * *") // Runs every 12:00 AM (midnight)
    @Transactional
    public void autoPurgeExpiredSoftDeletedResidents() {
        log.info("[ResidentScheduler] Checking soft-deleted residents exceeding retention threshold for permanent deletion...");
        try {
            ArchiveSettings settings = archiveSettingsRepository.findById(1L).orElse(null);
            if (settings == null || !Boolean.TRUE.equals(settings.getAutoDeleteEnabled())) {
                log.info("[ResidentScheduler] Auto-delete retention is currently disabled.");
                return;
            }

            int retentionDays = settings.getDeleteAfterDays() != null ? settings.getDeleteAfterDays() : 30;
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

            List<Resident> expiredResidents = residentRepository.findExpiredSoftDeletedResidents(cutoff);
            if (expiredResidents.isEmpty()) {
                log.info("[ResidentScheduler] No soft-deleted residents found exceeding {} days retention.", retentionDays);
                return;
            }

            int processedCount = 0;
            for (Resident r : expiredResidents) {
                try {
                    r.permanentDelete();
                    r.setStatus(ResidentStatus.INACTIVE);
                    residentRepository.save(r);
                    processedCount++;
                    log.info("[ResidentScheduler] Auto-transferred expired soft-deleted resident to permanent delete: ID {}", r.getResidentId());
                } catch (Exception e) {
                    log.warn("[ResidentScheduler] Failed to auto-permanent delete resident ID {}: {}", r.getResidentId(), e.getMessage());
                }
            }

            log.info("[ResidentScheduler] Midnight resident trash retention check completed. Processed {} resident(s).", processedCount);
        } catch (Exception e) {
            log.error("[ResidentScheduler] Error running auto-transfer resident task: {}", e.getMessage(), e);
        }
    }
}
