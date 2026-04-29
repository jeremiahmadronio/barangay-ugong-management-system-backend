package com.barangay.zshared.entity;

import java.time.LocalDateTime;

public interface Archivable {

    Boolean GetIsArchived();
    void setIsArchived(boolean isArchived);

    LocalDateTime GetArchiveTime();
    void setArchivedAt(LocalDateTime archivedAt);

    String archivedRemarks();
    void setArchiveRemarks(String archivedRemarks);


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
