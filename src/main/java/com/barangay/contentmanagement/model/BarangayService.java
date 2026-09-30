package com.barangay.contentmanagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cms_services")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BarangayService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "icon_name", nullable = false)
    private String iconName;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "VARCHAR(255) DEFAULT 'DOCUMENT'")
    private String type = "DOCUMENT";

    @Column(columnDefinition = "TEXT")
    private String description;

    private String fee;

    @Column(name = "processing_time")
    private String processingTime;

    private String location;

    @Column(name = "operating_days")
    private String operatingDays;

    @Column(name = "operating_hours")
    private String operatingHours;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(columnDefinition = "TEXT")
    private String steps;

    @Column(name = "claim_process", columnDefinition = "TEXT")
    private String claimProcess;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_published")
    private Boolean isPublished = true;

    @Column(name = "is_archived")
    private Boolean isArchived = false;
}
