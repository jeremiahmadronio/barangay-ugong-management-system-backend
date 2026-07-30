package com.barangay.zshared.repository;

import com.barangay.zshared.entity.ArchiveSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArchiveSettingsRepository extends JpaRepository<ArchiveSettings, Long> {
}
