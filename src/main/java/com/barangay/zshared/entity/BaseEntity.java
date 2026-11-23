package com.barangay.zshared.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Base mapped superclass providing standard lifecycle timestamps and soft/permanent deletion flags for entities.
 */
@MappedSuperclass
@Getter @Setter
public abstract class BaseEntity {

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_permanent_deleted")
    private Boolean isPermanentDeleted = false;

    @Column(name = "permanent_deleted_at")
    private LocalDateTime permanentDeletedAt;

    public Boolean getIsDeleted() {
        return isDeleted != null && isDeleted;
    }

    public Boolean getIsPermanentDeleted() {
        return isPermanentDeleted != null && isPermanentDeleted;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
        this.isPermanentDeleted = false;
    }

    public void permanentDelete() {
        this.isDeleted = false;
        this.isPermanentDeleted = true;
        this.permanentDeletedAt = LocalDateTime.now();
    }

    public void restoreDelete() {
        this.isDeleted = false;
        this.deletedAt = null;
        this.isPermanentDeleted = false;
        this.permanentDeletedAt = null;
    }
}
