package com.barangay.zshared.backup.service;

import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.backup.dto.BackupResponseDTO;
import com.barangay.zshared.backup.dto.BackupStatsDTO;
import com.barangay.zshared.backup.dto.TriggerBackupRequest;
import com.barangay.zshared.backup.model.BackupRecord;
import com.barangay.zshared.backup.model.BackupSchedule;
import com.barangay.zshared.backup.repository.BackupRecordRepository;
import com.barangay.zshared.backup.repository.BackupScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

/**
 * Service orchestrating PostgreSQL database backups and AWS S3 storage.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseBackupService {

    private final S3Client s3Client;
    private final BackupRecordRepository backupRecordRepository;
    private final BackupScheduleRepository backupScheduleRepository;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPass;

    @Value("${aws.s3.bucket}")
    private String s3Bucket;

    @Value("${aws.region:ap-southeast-1}")
    private String s3Region;

    @Value("${backup.storage-limit-gb:20.0}")
    private Double storageLimitGb;

    private final AtomicBoolean isBackupRunning = new AtomicBoolean(false);
    private String backupProgressStatus = "IDLE";

    public boolean isBackupInProgress() {
        return isBackupRunning.get();
    }

    public record BackupDownloadPayload(Resource resource, long contentLength) {}

    /**
     * Executes database dump, compresses with gzip, and uploads directly to AWS S3.
     */
    public BackupResponseDTO performBackup(TriggerBackupRequest request, String type, User actor, String ipAddress) {
        if (!isBackupRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("Another backup execution is currently running. Please wait.");
        }

        backupProgressStatus = "STARTING";
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String baseName = "ugong_backup_" + timestamp;

        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "serbisyoko_backups");
        File sqlDumpFile = tempDir.resolve(baseName + ".sql").toFile();
        File gzDumpFile = tempDir.resolve(baseName + ".sql.gz").toFile();

        try {
            Files.createDirectories(tempDir);

            backupProgressStatus = "DUMPING_DATABASE";
            log.info("Starting PostgreSQL pg_dump for backup -> {}", sqlDumpFile.getName());
            runPgDump(sqlDumpFile);

            backupProgressStatus = "COMPRESSING";
            log.info("Compressing SQL dump to gzip format -> {}", gzDumpFile.getName());
            compressFile(sqlDumpFile, gzDumpFile);

            long fileSizeKb = gzDumpFile.length() / 1024;
            String s3Key = "backups/" + gzDumpFile.getName();

            backupProgressStatus = "UPLOADING_TO_S3";
            log.info("Uploading {} ({} KB) to AWS S3 bucket: {}", gzDumpFile.getName(), fileSizeKb, s3Bucket);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(s3Key)
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .metadata(Map.of(
                            "backup-type", type != null ? type : "MANUAL",
                            "created-at", now.toString(),
                            "system", "Serbisyoko"
                    ))
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromFile(gzDumpFile));
            log.info("Successfully uploaded {} to AWS S3", s3Key);

            BackupRecord record = BackupRecord.builder()
                    .fileName(gzDumpFile.getName())
                    .label(request != null && request.getLabel() != null ? request.getLabel() : "Database Backup")
                    .s3Bucket(s3Bucket)
                    .s3Key(s3Key)
                    .fileSizeKb(fileSizeKb)
                    .backupType(type != null ? type : "MANUAL")
                    .status("ACTIVE")
                    .reason(request != null ? request.getReason() : null)
                    .triggeredBy(actor)
                    .createdAt(LocalDateTime.now())
                    .build();

            backupRecordRepository.save(record);

            backupProgressStatus = "COMPLETED";
            return toDTO(record);

        } catch (Exception e) {
            backupProgressStatus = "FAILED";
            log.error("Database backup workflow failed: {}", e.getMessage(), e);
            throw new RuntimeException("Database backup failed: " + e.getMessage(), e);
        } finally {
            if (sqlDumpFile.exists()) sqlDumpFile.delete();
            if (gzDumpFile.exists()) gzDumpFile.delete();
            isBackupRunning.set(false);
            backupProgressStatus = "IDLE";
        }
    }

    /**
     * Lists all backup files (aggregating S3 bucket objects and database records).
     */
    @Transactional(readOnly = true)
    public List<BackupResponseDTO> listAllBackups() {
        Map<String, BackupResponseDTO> backupsMap = new LinkedHashMap<>();

        List<BackupRecord> dbRecords = backupRecordRepository.findAllWithTriggeredBy();
        for (BackupRecord record : dbRecords) {
            backupsMap.put(record.getFileName(), toDTO(record));
        }

        try {
            if (s3Bucket != null && !s3Bucket.isBlank()) {
                ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                        .bucket(s3Bucket)
                        .prefix("backups/")
                        .build();

                ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

                for (S3Object s3Object : listResponse.contents()) {
                    String fullKey = s3Object.key();
                    String fileName = fullKey.contains("/") ? fullKey.substring(fullKey.lastIndexOf('/') + 1) : fullKey;

                    if (!backupsMap.containsKey(fileName)) {
                        LocalDateTime createdAt = LocalDateTime.ofInstant(s3Object.lastModified(), ZoneId.of("Asia/Manila"));
                        backupsMap.put(fileName, BackupResponseDTO.builder()
                                .fileName(fileName)
                                .label("S3 Cloud Object")
                                .s3Bucket(s3Bucket)
                                .fileSizeKb(s3Object.size() / 1024)
                                .type("MANUAL")
                                .status("ACTIVE")
                                .createdAt(createdAt)
                                .createdBy("AWS_S3")
                                .build());
                    }
                }
            }
        } catch (Throwable e) {
            log.warn("Could not list raw S3 objects directly from bucket: {}", e.getMessage());
        }

        List<BackupResponseDTO> list = new ArrayList<>(backupsMap.values());
        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return list;
    }

    /**
     * Downloads a backup file directly from AWS S3 with content length.
     */
    public BackupDownloadPayload downloadBackup(String fileName) {
        validateFileName(fileName);
        String s3Key = "backups/" + fileName;

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(s3Key)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Stream = s3Client.getObject(getRequest);
            long contentLength = s3Stream.response().contentLength() != null ? s3Stream.response().contentLength() : 0L;
            return new BackupDownloadPayload(new InputStreamResource(s3Stream), contentLength);
        } catch (Exception e) {
            log.error("Failed to download {} from S3: {}", fileName, e.getMessage());
            throw new RuntimeException("Could not download backup file from S3: " + e.getMessage());
        }
    }

    /**
     * Retrieves overall backup & storage statistics.
     */
    @Transactional(readOnly = true)
    public BackupStatsDTO getBackupStats() {
        double totalBytes = 0;
        try {
            if (s3Bucket != null && !s3Bucket.isBlank()) {
                ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                        .bucket(s3Bucket)
                        .prefix("backups/")
                        .build();

                for (S3Object obj : s3Client.listObjectsV2(listRequest).contents()) {
                    totalBytes += obj.size();
                }
            }
        } catch (Throwable e) {
            log.warn("Could not calculate S3 bucket size: {}", e.getMessage());
        }

        double usedGb = Math.round((totalBytes / (1024.0 * 1024.0 * 1024.0)) * 100.0) / 100.0;

        BackupSchedule schedule = backupScheduleRepository.findById(1L).orElse(null);
        String frequency = schedule != null && schedule.isEnabled() ? schedule.getFrequency() : "DISABLED";

        List<BackupRecord> records = backupRecordRepository.findAllByOrderByCreatedAtDesc();
        String lastStatus = records.isEmpty() ? "NONE" : records.get(0).getStatus();
        String lastDate = records.isEmpty() ? "N/A" : records.get(0).getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a"));

        return BackupStatsDTO.builder()
                .storageUsedGb(usedGb)
                .storageLimitGb(storageLimitGb)
                .autoBackupFrequency(frequency)
                .nextBackupTime(schedule != null ? formatSchedule(schedule) : "N/A")
                .lastBackupStatus(lastStatus)
                .lastBackupDate(lastDate)
                .isBackupInProgress(isBackupRunning.get())
                .backupProgressStatus(backupProgressStatus)
                .s3Bucket(s3Bucket)
                .s3Region(s3Region)
                .build();
    }

    private String formatSchedule(BackupSchedule s) {
        if (!s.isEnabled()) return "DISABLED";

        ZoneId manilaZone = ZoneId.of("Asia/Manila");
        LocalDateTime now = LocalDateTime.now(manilaZone);

        int h = s.getHour() != null ? s.getHour() : 0;
        int m = s.getMinute() != null ? s.getMinute() : 0;
        String freq = s.getFrequency() != null ? s.getFrequency().toUpperCase() : "DAILY";

        LocalDateTime next = now.withHour(h).withMinute(m).withSecond(0).withNano(0);

        if ("MONTHLY".equals(freq)) {
            int dom = 1;
            try {
                if (s.getDayOfMonth() != null) {
                    dom = Integer.parseInt(s.getDayOfMonth().trim());
                }
            } catch (Exception ignored) {}

            int validDom = Math.min(dom, next.toLocalDate().lengthOfMonth());
            next = next.withDayOfMonth(validDom);

            if (!next.isAfter(now)) {
                next = next.plusMonths(1);
                validDom = Math.min(dom, next.toLocalDate().lengthOfMonth());
                next = next.withDayOfMonth(validDom);
            }
        } else if ("WEEKLY".equals(freq)) {
            DayOfWeek targetDow = DayOfWeek.SUNDAY;
            try {
                if (s.getDayOfWeek() != null) {
                    targetDow = switch (s.getDayOfWeek().toUpperCase()) {
                        case "MON" -> DayOfWeek.MONDAY;
                        case "TUE" -> DayOfWeek.TUESDAY;
                        case "WED" -> DayOfWeek.WEDNESDAY;
                        case "THU" -> DayOfWeek.THURSDAY;
                        case "FRI" -> DayOfWeek.FRIDAY;
                        case "SAT" -> DayOfWeek.SATURDAY;
                        default -> DayOfWeek.SUNDAY;
                    };
                }
            } catch (Exception ignored) {}

            while (next.getDayOfWeek() != targetDow || !next.isAfter(now)) {
                next = next.plusDays(1);
            }
        } else {
            if (!next.isAfter(now)) {
                next = next.plusDays(1);
            }
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
        return next.format(formatter);
    }

    private void runPgDump(File targetFile) throws IOException, InterruptedException {
        String pgDumpBin = resolvePgDump();
        ParsedDbUrl parsed = parseDbUrl(dbUrl);

        ProcessBuilder pb = new ProcessBuilder(
                pgDumpBin,
                "-h", parsed.host,
                "-p", String.valueOf(parsed.port),
                "-U", dbUser != null ? dbUser.trim() : "postgres",
                "-F", "p",
                "-f", targetFile.getAbsolutePath(),
                parsed.database
        );

        if (dbPass != null && !dbPass.isBlank()) {
            pb.environment().put("PGPASSWORD", dbPass.trim());
        }
        pb.redirectErrorStream(true);

        Process p = pb.start();
        boolean finished = p.waitFor(10, TimeUnit.MINUTES);
        if (!finished || p.exitValue() != 0) {
            throw new IOException("pg_dump failed with exit code: " + (finished ? p.exitValue() : "TIMEOUT"));
        }
    }

    private void compressFile(File source, File destination) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(destination);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {

            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                gzos.write(buffer, 0, length);
            }
        }
    }

    private void validateFileName(String fileName) {
        if (fileName == null || fileName.isBlank() || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new SecurityException("Invalid file name.");
        }
    }

    private String resolvePgDump() {
        boolean isWin = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (isWin) {
            String[] commonPaths = {
                    "C:\\Program Files\\PostgreSQL\\17\\bin\\pg_dump.exe",
                    "C:\\Program Files\\PostgreSQL\\16\\bin\\pg_dump.exe",
                    "C:\\Program Files\\PostgreSQL\\15\\bin\\pg_dump.exe"
            };
            for (String p : commonPaths) {
                if (new File(p).exists()) return p;
            }
        }
        return "pg_dump";
    }

    private ParsedDbUrl parseDbUrl(String url) {
        try {
            String clean = url.replace("jdbc:postgresql://", "http://");
            URI uri = URI.create(clean);
            String path = uri.getPath();
            String db = (path != null && path.length() > 1) ? path.substring(1) : "serbisyoko";
            int port = uri.getPort() != -1 ? uri.getPort() : 5432;
            String host = uri.getHost() != null ? uri.getHost() : "localhost";
            return new ParsedDbUrl(host, port, db);
        } catch (Exception e) {
            return new ParsedDbUrl("localhost", 5432, "serbisyoko");
        }
    }

    private record ParsedDbUrl(String host, int port, String database) {}

    private BackupResponseDTO toDTO(BackupRecord r) {
        String createdByName = "SYSTEM";
        try {
            if (r.getTriggeredBy() != null) {
                createdByName = r.getTriggeredBy().getUsername();
            }
        } catch (Exception e) {
            createdByName = "SYSTEM";
        }

        String displayStatus = r.getStatus();
        if ("SUCCESS".equalsIgnoreCase(displayStatus)) {
            displayStatus = "ACTIVE";
        }

        return BackupResponseDTO.builder()
                .id(r.getId())
                .fileName(r.getFileName())
                .label(r.getLabel())
                .createdBy(createdByName)
                .createdAt(r.getCreatedAt())
                .fileSizeKb(r.getFileSizeKb())
                .type(r.getBackupType())
                .status(displayStatus)
                .s3Bucket(r.getS3Bucket())
                .build();
    }
}
