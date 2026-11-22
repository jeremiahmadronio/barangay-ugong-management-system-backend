package com.barangay.contentmanagement.repository;

import com.barangay.contentmanagement.model.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing key-value system configuration settings.
 */
@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
