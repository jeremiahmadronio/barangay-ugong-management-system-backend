package com.barangay.zshared.backup.repository;

import com.barangay.zshared.backup.model.BackupRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BackupRecordRepository extends JpaRepository<BackupRecord, Long> {

    @Query("SELECT b FROM BackupRecord b LEFT JOIN FETCH b.triggeredBy ORDER BY b.createdAt DESC")
    List<BackupRecord> findAllWithTriggeredBy();

    List<BackupRecord> findAllByOrderByCreatedAtDesc();

    Optional<BackupRecord> findByFileName(String fileName);
}


