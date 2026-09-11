package com.barangay.zshared.scheduler;

import com.barangay.cases.blotter.service.BlotterArchiveAndDeleteService;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.entity.ArchiveSettings;
import com.barangay.zshared.enumerated.OfficerStatus;
import com.barangay.zshared.enumerated.ResidentStatus;
import com.barangay.zshared.enumerated.UserStatus;
import com.barangay.zshared.repository.ArchiveSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveAutoDeleteScheduler {

    private final ArchiveSettingsRepository archiveSettingsRepository;
    private final ResidentRepository residentRepository;
    private final OfficerRepository officerRepository;
    private final UserRepository userRepository;
    private final CaseRepository caseRepository;
    private final BlotterArchiveAndDeleteService blotterArchiveAndDeleteService;

    @Scheduled(cron = "0 0 0 * * *") // Runs every 12:00 AM (midnight)
    @Transactional
    public void autoDeleteExpiredArchives() {
        log.info("Checking for expired archive items for permanent-delete transfer...");
        try {
            ArchiveSettings settings = archiveSettingsRepository.findById(1L).orElse(null);
            if (settings == null || !Boolean.TRUE.equals(settings.getAutoDeleteEnabled())) {
                log.info("Auto-delete feature is disabled.");
                return;
            }

            int retentionDays = settings.getDeleteAfterDays() != null ? settings.getDeleteAfterDays() : 30;
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

            // 1. Transfer Soft-Deleted Cases (Blotter, Lupon, and VAWC) to Permanent Delete State (Audit-Safe Purge)
            List<Case> expiredCases = caseRepository.findExpiredSoftDeletedCases(cutoff);
            for (Case c : expiredCases) {
                try {
                    blotterArchiveAndDeleteService.permanentDeleteCaseByFlag(c.getId());
                    log.info("Auto-transferred expired soft-deleted case to permanent delete: Case #{}", c.getCaseNumber());
                } catch (Exception e) {
                    log.warn("Could not auto-permanent delete case ID {}: {}", c.getId(), e.getMessage());
                }
            }

            // 2. Transfer Users to Permanent Delete State
            List<User> archivedUsers = userRepository.findAllByStatus(UserStatus.ARCHIVED);
            for (User u : archivedUsers) {
                if (!Boolean.TRUE.equals(u.getIsPermanentDeleted()) && u.getArchivedAt() != null && u.getArchivedAt().isBefore(cutoff)) {
                    u.permanentDelete();
                    userRepository.save(u);
                    log.info("Auto-transferred expired user archive to permanent delete: {}", u.getUsername());
                }
            }

            // 3. Transfer Officers to Permanent Delete State
            List<Officer> archivedOfficers = officerRepository.findAllByStatus(OfficerStatus.ARCHIVED);
            for (Officer o : archivedOfficers) {
                if (!Boolean.TRUE.equals(o.getIsPermanentDeleted()) && o.getUpdatedAt() != null && o.getUpdatedAt().isBefore(cutoff)) {
                    o.permanentDelete();
                    officerRepository.save(o);
                    log.info("Auto-transferred expired officer archive to permanent delete: ID {}", o.getId());
                }
            }

            // 4. Transfer Residents to Permanent Delete State
            List<Resident> archivedResidents = residentRepository.findAllByStatus(ResidentStatus.ARCHIVED);
            for (Resident r : archivedResidents) {
                if (!Boolean.TRUE.equals(r.getIsPermanentDeleted()) && r.getUpdatedAt() != null && r.getUpdatedAt().isBefore(cutoff)) {
                    r.permanentDelete();
                    residentRepository.save(r);
                    log.info("Auto-transferred expired resident archive to permanent delete: ID {}", r.getResidentId());
                }
            }
        } catch (Exception e) {
            log.error("Error running auto-transfer archive task: {}", e.getMessage());
        }
    }
}
