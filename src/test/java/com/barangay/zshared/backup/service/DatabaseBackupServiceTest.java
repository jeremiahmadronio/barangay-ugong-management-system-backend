package com.barangay.zshared.backup.service;

import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.backup.dto.BackupResponseDTO;
import com.barangay.zshared.backup.dto.BackupStatsDTO;
import com.barangay.zshared.backup.model.BackupRecord;
import com.barangay.zshared.backup.model.BackupSchedule;
import com.barangay.zshared.backup.repository.BackupRecordRepository;
import com.barangay.zshared.backup.repository.BackupScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseBackupService - BDD Unit Tests")
class DatabaseBackupServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private BackupRecordRepository backupRecordRepository;

    @Mock
    private BackupScheduleRepository backupScheduleRepository;

    @InjectMocks
    private DatabaseBackupService databaseBackupService;

    private static final String BUCKET_NAME = "test-backup-bucket";
    private static final String REGION = "ap-southeast-1";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(databaseBackupService, "s3Bucket", BUCKET_NAME);
        ReflectionTestUtils.setField(databaseBackupService, "s3Region", REGION);
        ReflectionTestUtils.setField(databaseBackupService, "storageLimitGb", 20.0);
    }

    private User createSampleUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("superadmin");
        user.setSystemEmail("admin@barangay.gov.ph");
        return user;
    }

    private BackupRecord createSampleRecord(String fileName, String status, String type) {
        return BackupRecord.builder()
                .id(101L)
                .fileName(fileName)
                .label("System Pre-Migration Backup")
                .s3Bucket(BUCKET_NAME)
                .s3Key("backups/" + fileName)
                .fileSizeKb(80240L)
                .backupType(type)
                .status(status)
                .triggeredBy(createSampleUser())
                .createdAt(LocalDateTime.of(2026, 11, 24, 8, 30))
                .build();
    }

    @Nested
    @DisplayName("GIVEN request to list all backups")
    class ListAllBackupsTests {

        @Test
        @DisplayName("WHEN database records and S3 files exist THEN returns aggregated backup list")
        void givenDbRecordsAndS3Objects_whenListAllBackups_thenReturnsCombinedList() {
            // GIVEN
            BackupRecord dbRecord = createSampleRecord("ugong_backup_2026-11-24.sql.gz", "ACTIVE", "MANUAL");
            given(backupRecordRepository.findAllWithTriggeredBy()).willReturn(List.of(dbRecord));

            S3Object s3Object = S3Object.builder()
                    .key("backups/ugong_backup_2026-11-24.sql.gz")
                    .size(82165760L)
                    .lastModified(Instant.now())
                    .build();

            ListObjectsV2Response s3Response = ListObjectsV2Response.builder()
                    .contents(List.of(s3Object))
                    .build();

            given(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).willReturn(s3Response);

            // WHEN
            List<BackupResponseDTO> result = databaseBackupService.listAllBackups();

            // THEN
            assertThat(result).isNotNull().hasSize(1);
            BackupResponseDTO item = result.get(0);
            assertThat(item.getFileName()).isEqualTo("ugong_backup_2026-11-24.sql.gz");
            assertThat(item.getLabel()).isEqualTo("System Pre-Migration Backup");
            assertThat(item.getType()).isEqualTo("MANUAL");
            assertThat(item.getStatus()).isEqualTo("ACTIVE");
            assertThat(item.getCreatedBy()).isEqualTo("superadmin");

            then(backupRecordRepository).should().findAllWithTriggeredBy();
            then(s3Client).should().listObjectsV2(any(ListObjectsV2Request.class));
        }

        @Test
        @DisplayName("WHEN S3 client throws exception THEN falls back safely to DB records without crashing")
        void givenS3Fails_whenListAllBackups_thenReturnsDbRecordsSafely() {
            // GIVEN
            BackupRecord dbRecord = createSampleRecord("ugong_backup_fallback.sql.gz", "ACTIVE", "SCHEDULED");
            given(backupRecordRepository.findAllWithTriggeredBy()).willReturn(List.of(dbRecord));
            given(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                    .willThrow(S3Exception.builder().message("Access Denied to S3").build());

            // WHEN
            List<BackupResponseDTO> result = databaseBackupService.listAllBackups();

            // THEN
            assertThat(result).isNotNull().hasSize(1);
            assertThat(result.get(0).getFileName()).isEqualTo("ugong_backup_fallback.sql.gz");
            assertThat(result.get(0).getType()).isEqualTo("SCHEDULED");
        }
    }

    @Nested
    @DisplayName("GIVEN request to get backup statistics")
    class GetBackupStatsTests {

        @Test
        @DisplayName("WHEN active schedule and storage usage exist THEN returns accurate stats")
        void givenActiveScheduleAndUsage_whenGetBackupStats_thenReturnsComputedStats() {
            // GIVEN
            S3Object s3Obj = S3Object.builder().size(1073741824L).build(); // 1 GB
            ListObjectsV2Response s3Response = ListObjectsV2Response.builder()
                    .contents(List.of(s3Obj))
                    .build();
            given(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).willReturn(s3Response);

            BackupSchedule schedule = BackupSchedule.builder()
                    .id(1L)
                    .frequency("DAILY")
                    .hour(8)
                    .minute(0)
                    .enabled(true)
                    .build();
            given(backupScheduleRepository.findById(1L)).willReturn(Optional.of(schedule));

            BackupRecord record = createSampleRecord("latest.sql.gz", "ACTIVE", "SCHEDULED");
            given(backupRecordRepository.findAllByOrderByCreatedAtDesc()).willReturn(List.of(record));

            // WHEN
            BackupStatsDTO stats = databaseBackupService.getBackupStats();

            // THEN
            assertThat(stats).isNotNull();
            assertThat(stats.getStorageUsedGb()).isEqualTo(1.0);
            assertThat(stats.getStorageLimitGb()).isEqualTo(20);
            assertThat(stats.getAutoBackupFrequency()).isEqualTo("DAILY");
            assertThat(stats.getNextBackupTime()).contains("at 08:00 AM");
            assertThat(stats.getLastBackupStatus()).isEqualTo("ACTIVE");
            assertThat(stats.getS3Bucket()).isEqualTo(BUCKET_NAME);
            assertThat(stats.getS3Region()).isEqualTo(REGION);
        }

        @Test
        @DisplayName("WHEN no records and schedule disabled THEN returns default stats")
        void givenDisabledScheduleAndNoRecords_whenGetBackupStats_thenReturnsDefaults() {
            // GIVEN
            given(s3Client.listObjectsV2(any(ListObjectsV2Request.class)))
                    .willReturn(ListObjectsV2Response.builder().contents(Collections.emptyList()).build());

            BackupSchedule schedule = BackupSchedule.builder().id(1L).enabled(false).build();
            given(backupScheduleRepository.findById(1L)).willReturn(Optional.of(schedule));
            given(backupRecordRepository.findAllByOrderByCreatedAtDesc()).willReturn(Collections.emptyList());

            // WHEN
            BackupStatsDTO stats = databaseBackupService.getBackupStats();

            // THEN
            assertThat(stats.getStorageUsedGb()).isEqualTo(0.0);
            assertThat(stats.getAutoBackupFrequency()).isEqualTo("DISABLED");
            assertThat(stats.getLastBackupStatus()).isEqualTo("NONE");
            assertThat(stats.getLastBackupDate()).isEqualTo("N/A");
        }
    }

    @Nested
    @DisplayName("GIVEN request to download backup file")
    class DownloadBackupTests {

        @Test
        @DisplayName("WHEN valid filename provided THEN streams file payload successfully from S3")
        void givenValidFileName_whenDownloadBackup_thenStreamsFile() {
            // GIVEN
            String validFile = "ugong_backup_2026-11-24.sql.gz";
            GetObjectResponse getObjectResponse = GetObjectResponse.builder()
                    .contentLength(1024L)
                    .build();

            ByteArrayInputStream byteStream = new ByteArrayInputStream("DUMMY_SQL_DATA".getBytes());
            ResponseInputStream<GetObjectResponse> responseInputStream = new ResponseInputStream<>(
                    getObjectResponse,
                    AbortableInputStream.create(byteStream)
            );

            given(s3Client.getObject(any(GetObjectRequest.class))).willReturn(responseInputStream);

            // WHEN
            DatabaseBackupService.BackupDownloadPayload payload = databaseBackupService.downloadBackup(validFile);

            // THEN
            assertThat(payload).isNotNull();
            assertThat(payload.resource()).isNotNull();
            assertThat(payload.contentLength()).isEqualTo(1024L);
            then(s3Client).should().getObject(any(GetObjectRequest.class));
        }

        @Test
        @DisplayName("WHEN malicious path traversal filename provided THEN rejects immediately with SecurityException")
        void givenMaliciousFileName_whenDownloadBackup_thenThrowsSecurityException() {
            // GIVEN
            String maliciousPath = "../../etc/passwd";

            // WHEN & THEN
            assertThatThrownBy(() -> databaseBackupService.downloadBackup(maliciousPath))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("Invalid file name");

            verifyNoInteractions(s3Client);
        }
    }
}
