package com.barangay.zshared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing system-wide configuration for automated archive retention and deletion policies.
 */
@Entity
@Table(name = "archive_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArchiveSettings {

    @Id
    private Long id = 1L;

    @Column(name = "auto_delete_enabled", nullable = false)
    private Boolean autoDeleteEnabled = true;

    @Column(name = "delete_after_days", nullable = false)
    private Integer deleteAfterDays = 30;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}
