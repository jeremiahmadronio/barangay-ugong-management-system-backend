package com.barangay.zshared.backup.model;

import com.barangay.usermanagement.user.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing database backup schedule configuration.
 */
@Entity
@Table(name = "backup_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupSchedule {

    @Id
    @Builder.Default
    private Long id = 1L;

    @Column(name = "frequency", nullable = false)
    @Builder.Default
    private String frequency = "DAILY";

    @Column(name = "hour", nullable = false)
    @Builder.Default
    private Integer hour = 0;

    @Column(name = "minute", nullable = false)
    @Builder.Default
    private Integer minute = 0;

    @Column(name = "day_of_week")
    private String dayOfWeek;

    @Column(name = "day_of_month")
    private String dayOfMonth;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "roles", "department"})
    private User createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
