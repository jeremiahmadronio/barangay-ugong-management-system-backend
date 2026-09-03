package com.barangay.zshared.scheduler;

import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.entity.ArchiveSettings;
import com.barangay.zshared.enumerated.UserStatus;
import com.barangay.zshared.repository.ArchiveSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled job for User Account management:
 * Executes daily at 12:00 AM (midnight) to automatically transition expired
 * soft-deleted user accounts in the trash vault into permanent-delete status
 * based on Barangay Archive Settings retention days.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserScheduler {

    private final ArchiveSettingsRepository archiveSettingsRepository;
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 0 * * *") // Runs every 12:00 AM (midnight)
    @Transactional
    public void autoPurgeExpiredSoftDeletedUsers() {
        log.info("[UserScheduler] Checking soft-deleted users exceeding retention threshold for permanent deletion...");
        try {
            ArchiveSettings settings = archiveSettingsRepository.findById(1L).orElse(null);
            if (settings == null || !Boolean.TRUE.equals(settings.getAutoDeleteEnabled())) {
                log.info("[UserScheduler] Auto-delete retention is currently disabled.");
                return;
            }

            int retentionDays = settings.getDeleteAfterDays() != null ? settings.getDeleteAfterDays() : 30;
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

            List<User> expiredUsers = userRepository.findExpiredSoftDeletedUsers(cutoff);
            if (expiredUsers.isEmpty()) {
                log.info("[UserScheduler] No soft-deleted users found exceeding {} days retention.", retentionDays);
                return;
            }

            int processedCount = 0;
            for (User u : expiredUsers) {
                try {
                    u.permanentDelete();
                    u.setStatus(UserStatus.INACTIVE);
                    u.setIsLocked(true);
                    userRepository.save(u);
                    processedCount++;
                    log.info("[UserScheduler] Auto-transferred expired soft-deleted user to permanent delete: {} (ID: {})", u.getUsername(), u.getId());
                } catch (Exception e) {
                    log.warn("[UserScheduler] Failed to auto-permanent delete user ID {}: {}", u.getId(), e.getMessage());
                }
            }

            log.info("[UserScheduler] Midnight user trash retention check completed. Processed {} user(s).", processedCount);
        } catch (Exception e) {
            log.error("[UserScheduler] Error running auto-transfer user task: {}", e.getMessage(), e);
        }
    }
}
