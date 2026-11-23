package com.barangay.zshared.scheduler;

import com.barangay.clearance.model.IssuedCertificate;
import com.barangay.clearance.repository.IssuedCertificateRepository;
import com.barangay.clearance.service.ClearanceArchive;
import com.barangay.zshared.entity.ArchiveSettings;
import com.barangay.zshared.enumerated.ClearanceStatus;
import com.barangay.zshared.repository.ArchiveSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled jobs for clearance validity tracking and expired trash certificate retention.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClearanceScheduler {

    private final ArchiveSettingsRepository archiveSettingsRepository;
    private final IssuedCertificateRepository issuedCertificateRepository;
    private final ClearanceArchive clearanceArchive;

    /**
     * Executes daily at midnight to permanently delete expired soft-deleted certificates.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void autoDeleteExpiredTrashCertificates() {
        log.info("[ClearanceScheduler] Checking soft-deleted certificates exceeding retention threshold for permanent deletion...");
        try {
            ArchiveSettings settings = archiveSettingsRepository.findById(1L).orElse(null);
            if (settings == null || !Boolean.TRUE.equals(settings.getAutoDeleteEnabled())) {
                log.info("[ClearanceScheduler] Auto-delete retention is currently disabled.");
                return;
            }

            int retentionDays = settings.getDeleteAfterDays() != null ? settings.getDeleteAfterDays() : 30;
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

            List<IssuedCertificate> expiredCertificates = issuedCertificateRepository.findExpiredSoftDeletedCertificates(cutoff);
            if (expiredCertificates.isEmpty()) {
                log.info("[ClearanceScheduler] No soft-deleted certificates found exceeding {} days retention.", retentionDays);
                return;
            }

            int processedCount = 0;
            for (IssuedCertificate ic : expiredCertificates) {
                try {
                    clearanceArchive.permanentDeleteCertificate(ic.getId());
                    processedCount++;
                    log.info("[ClearanceScheduler] Auto-permanently deleted expired certificate #{} (ID: {})", ic.getCertNumber(), ic.getId());
                } catch (Exception e) {
                    log.warn("[ClearanceScheduler] Failed to auto-permanently delete certificate ID {}: {}", ic.getId(), e.getMessage());
                }
            }

            log.info("[ClearanceScheduler] Midnight trash retention check completed. Processed {} certificate(s).", processedCount);
        } catch (Exception e) {
            log.error("[ClearanceScheduler] Error executing midnight trash retention scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Executes daily at midnight to update active certificates past their expiration date to EXPIRED.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void autoExpireActiveCertificates() {
        log.info("[ClearanceScheduler] Checking for active certificates past their expiration date...");
        try {
            List<IssuedCertificate> activeList = issuedCertificateRepository.findAllActiveCertificates();
            LocalDate today = LocalDate.now();
            int expiredCount = 0;

            for (IssuedCertificate ic : activeList) {
                if (ic.getExpiryDate() != null && ic.getExpiryDate().isBefore(today) && ic.getStatus() == ClearanceStatus.ACTIVE) {
                    ic.setStatus(ClearanceStatus.EXPIRED);
                    issuedCertificateRepository.save(ic);
                    expiredCount++;
                    log.info("[ClearanceScheduler] Automatically marked certificate #{} as EXPIRED (Expiry date: {})", ic.getCertNumber(), ic.getExpiryDate());
                }
            }

            if (expiredCount > 0) {
                log.info("[ClearanceScheduler] Expiration check completed. Marked {} certificate(s) as EXPIRED.", expiredCount);
            }
        } catch (Exception e) {
            log.error("[ClearanceScheduler] Error executing certificate expiration scheduler: {}", e.getMessage(), e);
        }
    }
}
