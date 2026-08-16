package com.barangay.ftjs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barangay.ftjs.model.FtjsSystemSetting;

@Repository
/**
 * Spring Data JPA Repository for FTJS global retention settings.
 */
public interface FtjsSystemSettingRepository extends JpaRepository<FtjsSystemSetting, String> {
}
