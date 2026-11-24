package com.barangay.zshared.backup.controller;

import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.audit.service.AuditLogService;
import com.barangay.zshared.backup.dto.BackupResponseDTO;
import com.barangay.zshared.backup.dto.BackupStatsDTO;
import com.barangay.zshared.backup.dto.TriggerBackupRequest;
import com.barangay.zshared.backup.dto.UpdateBackupScheduleRequest;
import com.barangay.zshared.backup.model.BackupSchedule;
import com.barangay.zshared.backup.service.DatabaseBackupService;
import com.barangay.zshared.backup.service.ScheduleBackupService;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import com.barangay.zshared.security.CustomUserDetails;
import com.barangay.zshared.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller handling PostgreSQL database backup endpoints and audit logging.
 * Restricted to SYSTEM_ADMIN and ROOT_ADMIN roles.
 */
@RestController
@RequestMapping("/api/v1/admin/backups")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Database Backup", description = "Endpoints for managing PostgreSQL backups and AWS S3 storage")
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'ROOT_ADMIN')")
public class BackupController {

    private final DatabaseBackupService backupService;
    private final ScheduleBackupService scheduleBackupService;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;

    @GetMapping("/schedule")
    @Operation(summary = "Get current automated database backup schedule")
    public ResponseEntity<BackupSchedule> getBackupSchedule() {
        return ResponseEntity.ok(scheduleBackupService.getSchedule());
    }

    @PutMapping("/schedule")
    @Operation(summary = "Update automated database backup schedule and refresh timer")
    public ResponseEntity<BackupSchedule> updateBackupSchedule(
            @Valid @RequestBody UpdateBackupScheduleRequest request,
            @AuthenticationPrincipal CustomUserDetails actor,
            HttpServletRequest httpRequest
    ) {
        User user = actor != null ? actor.user() : securityUtils.getCurrentUser();
        String ipAddress = httpRequest.getRemoteAddr();

        BackupSchedule updated = scheduleBackupService.updateSchedule(request, user);

        if (user != null) {
            auditLogService.log(user, AuditModule.SECURITY, "BACKUP_SCHEDULE_UPDATED",
                    Severity.INFO, "Backup schedule updated: frequency=" + updated.getFrequency() +
                            ", time=" + updated.getHour() + ":" + updated.getMinute() +
                            ", enabled=" + updated.isEnabled(), ipAddress, null, null);
        }

        return ResponseEntity.ok(updated);
    }

    @PostMapping("/trigger")
    @Operation(summary = "Trigger a manual database backup to AWS S3")
    public ResponseEntity<BackupResponseDTO> triggerManualBackup(
            @RequestBody(required = false) TriggerBackupRequest request,
            @AuthenticationPrincipal CustomUserDetails actor,
            HttpServletRequest httpRequest
    ) {
        User user = actor != null ? actor.user() : securityUtils.getCurrentUser();
        String ipAddress = httpRequest.getRemoteAddr();

        try {
            BackupResponseDTO response = backupService.performBackup(request, "MANUAL", user, ipAddress);

            if (user != null) {
                auditLogService.log(user, AuditModule.SECURITY, "DATABASE_BACKUP_COMPLETED",
                        Severity.INFO, "Backup successfully created and uploaded to S3: " + response.getFileName(), ipAddress, null, null);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            if (user != null) {
                auditLogService.log(user, AuditModule.SECURITY, "DATABASE_BACKUP_FAILED",
                        Severity.CRITICAL, "Database backup failed: " + e.getMessage(), ipAddress, null, null);
            }
            throw e;
        }
    }

    @GetMapping
    @Operation(summary = "List all database backups (Database records + AWS S3 objects)")
    public ResponseEntity<List<BackupResponseDTO>> listAllBackups() {
        return ResponseEntity.ok(backupService.listAllBackups());
    }

    @GetMapping("/stats")
    @Operation(summary = "Get backup storage usage and scheduler statistics")
    public ResponseEntity<BackupStatsDTO> getBackupStats() {
        return ResponseEntity.ok(backupService.getBackupStats());
    }

    @GetMapping("/{fileName}/download")
    @Operation(summary = "Download a database backup file directly from AWS S3")
    public ResponseEntity<Resource> downloadBackup(
            @PathVariable String fileName,
            @AuthenticationPrincipal CustomUserDetails actor,
            HttpServletRequest httpRequest
    ) {
        User user = actor != null ? actor.user() : securityUtils.getCurrentUser();
        String ipAddress = httpRequest.getRemoteAddr();

        DatabaseBackupService.BackupDownloadPayload payload = backupService.downloadBackup(fileName);

        if (user != null) {
            auditLogService.log(user, AuditModule.SECURITY, "DATABASE_BACKUP_DOWNLOADED",
                    Severity.INFO, "Database backup downloaded: " + fileName, ipAddress, null, null);
        }

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM);
        if (payload.contentLength() > 0) {
            builder.contentLength(payload.contentLength());
        }
        return builder.body(payload.resource());
    }
}
