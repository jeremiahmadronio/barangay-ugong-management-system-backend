package com.barangay.contentmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ServiceDto {

    @NotBlank(message = "Icon name is required")
    private String iconName;

    @NotBlank(message = "Title is required")
    private String title;

    private String type = "DOCUMENT"; // DOCUMENT or SERVICE

    private String description;
    private String fee;
    private String processingTime;
    private String location;
    private String operatingDays;
    private String operatingHours;

    // JSON arrays as string
    private String requirements;
    private String steps;
    private String claimProcess;
}
