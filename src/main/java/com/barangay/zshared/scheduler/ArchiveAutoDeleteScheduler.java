package com.barangay.zshared.scheduler;

import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.user_management.user.model.User;
import com.barangay.user_management.user.repository.UserRepository;
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

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void autoDeleteExpiredArchives() {
        log.info("Checking for expired archive items for auto-deletion...");
        try {
            ArchiveSettings settings = archiveSettingsRepository.findById(1L).orElse(null);
            if (settings == null || !Boolean.TRUE.equals(settings.getAutoDeleteEnabled())) {
                log.info("Auto-delete feature is disabled.");
                return;
            }

            int retentionDays = settings.getDeleteAfterDays() != null ? settings.getDeleteAfterDays() : 30;
            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

            // Delete Users
            List<User> archivedUsers = userRepository.findAllByStatus(UserStatus.ARCHIVED);
            for (User u : archivedUsers) {
                if (u.getArchivedAt() != null && u.getArchivedAt().isBefore(cutoff)) {
                    userRepository.delete(u);
                    log.info("Auto-deleted expired user archive: {}", u.getUsername());
                }
            }

            // Delete Officers
            List<Officer> archivedOfficers = officerRepository.findAllByStatus(OfficerStatus.ARCHIVED);
            for (Officer o : archivedOfficers) {
                if (o.getUpdatedAt() != null && o.getUpdatedAt().isBefore(cutoff)) {
                    officerRepository.delete(o);
                    log.info("Auto-deleted expired officer archive: ID {}", o.getId());
                }
            }

            // Delete Residents
            List<Resident> archivedResidents = residentRepository.findAllByStatus(ResidentStatus.ARCHIVED);
            for (Resident r : archivedResidents) {
                if (r.getUpdatedAt() != null && r.getUpdatedAt().isBefore(cutoff)) {
                    residentRepository.delete(r);
                    log.info("Auto-deleted expired resident archive: ID {}", r.getResidentId());
                }
            }
        } catch (Exception e) {
            log.error("Error running auto-delete archive task: {}", e.getMessage());
        }
    }
}
