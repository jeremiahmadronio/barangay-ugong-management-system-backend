package com.barangay.zshared.backup.repository;

import com.barangay.zshared.backup.model.BackupSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BackupScheduleRepository extends JpaRepository<BackupSchedule, Long> {
}
