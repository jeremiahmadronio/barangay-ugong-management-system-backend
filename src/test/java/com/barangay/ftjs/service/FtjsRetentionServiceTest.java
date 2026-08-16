package com.barangay.ftjs.service;

import com.barangay.ftjs.dto.RetentionSettingDto;
import com.barangay.ftjs.model.FtjsSystemSetting;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.ftjs.repository.FtjsSystemSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FtjsRetentionServiceTest {

    @Mock
    private FtjsSystemSettingRepository ftjsSettingRepository;

    @Mock
    private FirstTimeJobSeekerRepository ftjsRepository;

    @InjectMocks
    private FtjsRetentionService ftjsRetentionService;

    @Nested
    @DisplayName("getCurrentRetentionSetting Method Tests")
    class GetCurrentRetentionSettingTests {

        @Test
        @DisplayName("shouldReturnExistingSetting_whenSettingIsInDatabase")
        void shouldReturnExistingSetting_whenSettingIsInDatabase() {
            // Given
            FtjsSystemSetting setting = new FtjsSystemSetting("RETENTION_PERIOD", "60");
            when(ftjsSettingRepository.findById("RETENTION_PERIOD")).thenReturn(Optional.of(setting));

            // When
            RetentionSettingDto.Response response = ftjsRetentionService.getCurrentRetentionSetting();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.retentionPeriod()).isEqualTo("60");
            verify(ftjsSettingRepository, times(1)).findById("RETENTION_PERIOD");
        }

        @Test
        @DisplayName("shouldReturnDefaultSetting_whenSettingNotFoundInDatabase")
        void shouldReturnDefaultSetting_whenSettingNotFoundInDatabase() {
            // Given
            when(ftjsSettingRepository.findById("RETENTION_PERIOD")).thenReturn(Optional.empty());

            // When
            RetentionSettingDto.Response response = ftjsRetentionService.getCurrentRetentionSetting();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.retentionPeriod()).isEqualTo("30");
        }
    }

    @Nested
    @DisplayName("updateRetentionSetting Method Tests")
    class UpdateRetentionSettingTests {

        @Test
        @DisplayName("shouldUpdateSetting_whenSettingAlreadyExists")
        void shouldUpdateSetting_whenSettingAlreadyExists() {
            // Given
            FtjsSystemSetting setting = new FtjsSystemSetting("RETENTION_PERIOD", "30");
            when(ftjsSettingRepository.findById("RETENTION_PERIOD")).thenReturn(Optional.of(setting));

            RetentionSettingDto.Request request = new RetentionSettingDto.Request("90");

            // When
            ftjsRetentionService.updateRetentionSetting(request);

            // Then
            assertThat(setting.getSettingValue()).isEqualTo("90");
            verify(ftjsSettingRepository, times(1)).save(setting);
        }
    }

    @Nested
    @DisplayName("cleanupExpiredDeletedRecords Scheduled Cleanup Tests")
    class CleanupExpiredDeletedRecordsTests {

        @Test
        @DisplayName("shouldCleanupRecords_whenRetentionPeriodIsNumber")
        void shouldCleanupRecords_whenRetentionPeriodIsNumber() {
            // Given
            FtjsSystemSetting setting = new FtjsSystemSetting("RETENTION_PERIOD", "30");
            when(ftjsSettingRepository.findById("RETENTION_PERIOD")).thenReturn(Optional.of(setting));

            // When
            ftjsRetentionService.cleanupExpiredDeletedRecords();

            // Then
            verify(ftjsRepository, times(1)).deleteChildAffidavitsExpired(any(LocalDateTime.class));
            verify(ftjsRepository, times(1)).deleteChildTimelinesExpired(any(LocalDateTime.class));
            verify(ftjsRepository, times(1)).deleteChildNotesExpired(any(LocalDateTime.class));
            verify(ftjsRepository, times(1)).hardDeleteExpiredRecords(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("shouldDoNothing_whenRetentionPeriodIsManual")
        void shouldDoNothing_whenRetentionPeriodIsManual() {
            // Given
            FtjsSystemSetting setting = new FtjsSystemSetting("RETENTION_PERIOD", "MANUAL");
            when(ftjsSettingRepository.findById("RETENTION_PERIOD")).thenReturn(Optional.of(setting));

            // When
            ftjsRetentionService.cleanupExpiredDeletedRecords();

            // Then
            verify(ftjsRepository, never()).hardDeleteExpiredRecords(any());
        }
    }
}
