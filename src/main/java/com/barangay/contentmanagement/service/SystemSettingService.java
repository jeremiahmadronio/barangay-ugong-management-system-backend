package com.barangay.contentmanagement.service;

import com.barangay.contentmanagement.model.SystemSetting;
import com.barangay.contentmanagement.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service managing global application and portal system configuration parameters.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    /**
     * Retrieves all portal configuration settings in key-value format.
     *
     * @return map of setting keys and values
     */
    @Transactional(readOnly = true)
    public Map<String, String> getAllSettings() {
        List<SystemSetting> settingsList = systemSettingRepository.findAll();
        Map<String, String> settingsMap = new HashMap<>();
        for (SystemSetting setting : settingsList) {
            settingsMap.put(setting.getSettingKey(), setting.getSettingValue());
        }
        settingsMap.putIfAbsent("global_mfa_enabled", "true");
        return settingsMap;
    }

    /**
     * Updates and saves system configuration key-value pairs.
     *
     * @param newSettings map of updated settings
     * @return updated settings map
     */
    public Map<String, String> updateSettings(Map<String, String> newSettings) {
        for (Map.Entry<String, String> entry : newSettings.entrySet()) {
            SystemSetting setting = systemSettingRepository.findById(entry.getKey())
                    .orElse(new SystemSetting(entry.getKey(), null));
            setting.setSettingValue(entry.getValue());
            systemSettingRepository.save(setting);
        }
        return newSettings;
    }
}
