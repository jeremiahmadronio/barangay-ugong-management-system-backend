package com.barangay.contentmanagement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing barangay community services and clearance guidelines displayed on the portal.
 */
@Entity
@Table(
    name = "cms_services",
    indexes = {
        @Index(name = "idx_cms_services_created_at", columnList = "created_at"),
        @Index(name = "idx_cms_services_is_published", columnList = "is_published"),
        @Index(name = "idx_cms_services_is_archived", columnList = "is_archived"),
        @Index(name = "idx_cms_services_type", columnList = "type")
    }
)
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
