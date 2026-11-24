package com.barangay.zshared.backup.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO representing overall backup and storage statistics.
 */
@Data
@Builder
public class BackupStatsDTO {
    private Double storageUsedGb;
    private Double storageLimitGb;
    private String autoBackupFrequency;
    private String nextBackupTime;
    private String lastBackupStatus;
    private String lastBackupDate;
    private Boolean isBackupInProgress;
    private String backupProgressStatus;
    private String s3Bucket;
    private String s3Region;
}
