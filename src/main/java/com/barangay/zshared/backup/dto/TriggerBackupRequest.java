package com.barangay.zshared.backup.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for triggering manual database backup.
 */
@Data
public class TriggerBackupRequest {
    private String label;
    private String reason;
}
