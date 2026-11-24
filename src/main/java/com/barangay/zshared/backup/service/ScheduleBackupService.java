package com.barangay.zshared.backup.service;

import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.backup.dto.TriggerBackupRequest;
import com.barangay.zshared.backup.dto.UpdateBackupScheduleRequest;
import com.barangay.zshared.backup.model.BackupSchedule;
import com.barangay.zshared.backup.repository.BackupScheduleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;

/**
 * Service that dynamically schedules automated PostgreSQL database backups to AWS S3
 * and manages backup schedule settings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleBackupService {

    private final TaskScheduler taskScheduler;
    private final BackupScheduleRepository scheduleRepository;
    private final DatabaseBackupService backupService;

    private ScheduledFuture<?> scheduledTask;

    @PostConstruct
    public void init() {
        refreshSchedule();
    }

    /**
     * Retrieves the current backup schedule.
     */
    @Transactional(readOnly = true)
    public BackupSchedule getSchedule() {
        BackupSchedule schedule = scheduleRepository.findById(1L)
                .orElseGet(() -> BackupSchedule.builder()
                        .id(1L)
                        .frequency("DAILY")
                        .hour(0)
                        .minute(0)
                        .enabled(true)
                        .build());
        schedule.setCreatedBy(null);
        return schedule;
    }

    /**
     * Updates the backup schedule and re-arms the scheduler.
     */
    @Transactional
    public BackupSchedule updateSchedule(UpdateBackupScheduleRequest request, User actor) {
        BackupSchedule schedule = scheduleRepository.findById(1L)
                .orElseGet(() -> BackupSchedule.builder().id(1L).build());

        if (request.getFrequency() != null) schedule.setFrequency(request.getFrequency().toUpperCase());
        if (request.getHour() != null) schedule.setHour(request.getHour());
        if (request.getMinute() != null) schedule.setMinute(request.getMinute());
        if (request.getDayOfWeek() != null) schedule.setDayOfWeek(request.getDayOfWeek().toUpperCase());
        if (request.getDayOfMonth() != null) schedule.setDayOfMonth(request.getDayOfMonth());
        if (request.getEnabled() != null) schedule.setEnabled(request.getEnabled());
        schedule.setCreatedBy(actor);

        BackupSchedule saved = scheduleRepository.save(schedule);
        refreshSchedule();

        saved.setCreatedBy(null);
        return saved;
    }

    /**
     * Cancels existing scheduled backup task and schedules a new one
     * according to the latest BackupSchedule database settings.
     */
    public synchronized void refreshSchedule() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            log.info("[ScheduleBackupService] Previous backup schedule cancelled.");
        }

        BackupSchedule config = scheduleRepository.findById(1L)
                .orElseGet(() -> {
                    BackupSchedule def = BackupSchedule.builder()
                            .id(1L)
                            .frequency("DAILY")
                            .hour(0)
                            .minute(0)
                            .enabled(true)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return scheduleRepository.save(def);
                });

        if (!config.isEnabled()) {
            log.info("[ScheduleBackupService] Automated database backups are currently DISABLED.");
            return;
        }

        String cronExpression = buildCron(config);
        log.info("[ScheduleBackupService] Scheduling automated backup -> Cron: '{}' (Asia/Manila)", cronExpression);

        scheduledTask = taskScheduler.schedule(() -> {
            log.info("[ScheduleBackupService] Triggering automated scheduled backup to AWS S3...");
            try {
                TriggerBackupRequest request = new TriggerBackupRequest();
                request.setLabel("Automated " + config.getFrequency() + " Backup");
                request.setReason("Scheduled Execution");
                backupService.performBackup(request, "SCHEDULED", null, "127.0.0.1");
            } catch (Exception e) {
                log.error("[ScheduleBackupService] Error during scheduled backup execution: {}", e.getMessage(), e);
            }
        }, new CronTrigger(cronExpression, TimeZone.getTimeZone("Asia/Manila")));
    }

    /**
     * Builds standard 6-field Spring Cron expression (second, minute, hour, day-of-month, month, day-of-week).
     */
    private String buildCron(BackupSchedule config) {
        int minute = config.getMinute() != null ? config.getMinute() : 0;
        int hour = config.getHour() != null ? config.getHour() : 0;

        if ("WEEKLY".equalsIgnoreCase(config.getFrequency())) {
            String dow = config.getDayOfWeek() != null && !config.getDayOfWeek().isBlank() ? config.getDayOfWeek() : "SUN";
            return String.format("0 %d %d * * %s", minute, hour, dow);
        }
        if ("MONTHLY".equalsIgnoreCase(config.getFrequency())) {
            String dom = config.getDayOfMonth() != null && !config.getDayOfMonth().isBlank() ? config.getDayOfMonth() : "1";
            return String.format("0 %d %d %s * *", minute, hour, dom);
        }
        // Default: DAILY
        return String.format("0 %d %d * * *", minute, hour);
    }
}
