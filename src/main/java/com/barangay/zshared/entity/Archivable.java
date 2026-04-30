package com.barangay.zshared.entity;

import java.time.LocalDateTime;

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

    default void unarchive(String remarks) {
        setIsArchived(false);
        setArchivedAt(null);
        setArchiveRemarks(remarks);
    }

    default void unarchive() {
        setIsArchived(false);
        setArchivedAt(null);
        setArchiveRemarks(null);
    }

}
