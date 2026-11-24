package com.barangay.zshared.backup.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request payload for configuring automated backup schedule.
 */
@Data
public class UpdateBackupScheduleRequest {

    @Pattern(regexp = "DAILY|WEEKLY|MONTHLY", message = "Frequency must be DAILY, WEEKLY, or MONTHLY")
    private String frequency;

    @Min(0)
    @Max(23)
    private Integer hour;

    @Min(0)
    @Max(59)
    private Integer minute;

    private String dayOfWeek;  // e.g. MON, TUE, WED, THU, FRI, SAT, SUN
    private String dayOfMonth; // 1-31

    private Boolean enabled;
}
