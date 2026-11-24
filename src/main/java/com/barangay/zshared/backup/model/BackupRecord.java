package com.barangay.zshared.backup.model;

import com.barangay.usermanagement.user.model.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity tracking history and metadata of database backup executions.
 */
@Entity
@Table(name = "backup_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "label")
    private String label;

    @Column(name = "s3_bucket")
    private String s3Bucket;

    @Column(name = "s3_key")
    private String s3Key;

    @Column(name = "file_size_kb")
    private Long fileSizeKb;

    @Column(name = "backup_type", nullable = false)
    @Builder.Default
    private String backupType = "MANUAL";

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "reason")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "roles", "department"})
    private User triggeredBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
