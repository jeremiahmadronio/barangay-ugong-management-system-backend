package com.barangay.zshared.backup.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO representing database backup file details in responses.
 */
@Data
@Builder
public class BackupResponseDTO {
    private Long id;
    private String fileName;
    private String label;
    private String createdBy;
    private LocalDateTime createdAt;
    private Long fileSizeKb;
    private String type;
    private String status;
    private LocalDateTime deletedAt;
    private LocalDateTime scheduledPurgeAt;
    private Long daysRemaining;
    private String s3Bucket;
}
