package com.barangay.contentmanagement.service;

import com.barangay.contentmanagement.model.SystemSetting;
import com.barangay.contentmanagement.repository.SystemSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * BDD Unit tests for {@link SystemSettingService}.
 * Verifies retrieval of portal configurations and updates of key-value pairs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SystemSettingService BDD Tests")
class SystemSettingServiceTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @InjectMocks
    private SystemSettingService systemSettingService;

    @Nested
    @DisplayName("Settings Retrieval Tests")
    class SettingsRetrievalTests {

        @Test
        @DisplayName("givenSettingsInDatabase_whenGetAllSettings_thenReturnPopulatedMapWithDefaultMfa")
        void givenSettingsInDatabase_whenGetAllSettings_thenReturnPopulatedMapWithDefaultMfa() {
            // Given
            SystemSetting setting = new SystemSetting("barangay_name", "Barangay Guadalupe");
            given(systemSettingRepository.findAll()).willReturn(List.of(setting));

            // When
            Map<String, String> result = systemSettingService.getAllSettings();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.get("barangay_name")).isEqualTo("Barangay Guadalupe");
            assertThat(result.get("global_mfa_enabled")).isEqualTo("true");
            then(systemSettingRepository).should().findAll();
        }
    }

    @Nested
    @DisplayName("Settings Update Tests")
    class SettingsUpdateTests {

        @Test
        @DisplayName("givenNewSettingsMap_whenUpdateSettings_thenPersistEachSetting")
        void givenNewSettingsMap_whenUpdateSettings_thenPersistEachSetting() {
            // Given
            Map<String, String> newSettings = Map.of(
                    "contact_number", "09123456789",
                    "office_hours", "8:00 AM - 5:00 PM"
            );

            given(systemSettingRepository.findById("contact_number")).willReturn(Optional.empty());
            given(systemSettingRepository.findById("office_hours")).willReturn(Optional.empty());
            given(systemSettingRepository.save(any(SystemSetting.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            Map<String, String> result = systemSettingService.updateSettings(newSettings);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get("contact_number")).isEqualTo("09123456789");
            then(systemSettingRepository).should(times(2)).save(any(SystemSetting.class));
        }
    }
}
