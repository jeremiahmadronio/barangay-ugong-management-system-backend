package com.barangay.zshared.entity;

import java.time.LocalDateTime;

/**
 * Interface contract for entities that support archival operations and tracking.
 */
public interface Archivable {

    Boolean getIsArchived();
    void setIsArchived(Boolean isArchived);

    LocalDateTime getArchivedAt();
    void setArchivedAt(LocalDateTime archivedAt);

    String getArchiveRemarks();
    void setArchiveRemarks(String archiveRemarks);

    default void archive(String remarks) {
        setIsArchived(true);
        setArchivedAt(LocalDateTime.now());
        setArchiveRemarks(remarks);
    }
}
