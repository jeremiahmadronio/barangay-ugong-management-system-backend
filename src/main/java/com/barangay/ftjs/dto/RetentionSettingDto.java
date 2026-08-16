package com.barangay.ftjs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO encapsulating FTJS retention period configuration requests and responses.
 */
public class RetentionSettingDto {

    public record Request(
            @NotBlank(message = "Retention period is required")
            @Pattern(regexp = "^(MANUAL|1|3|7|15|30)$", message = "Invalid retention period option")
            String retentionPeriod
    ) {}

    public record Response(
            String retentionPeriod
    ) {}
}