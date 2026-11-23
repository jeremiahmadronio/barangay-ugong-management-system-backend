package com.barangay.zshared.scheduler;

import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.zshared.entity.ArchiveSettings;
import com.barangay.zshared.repository.ArchiveSettingsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * BDD Unit tests for {@link OfficerScheduler}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OfficerScheduler BDD Tests")
class OfficerSchedulerTest {

    @Mock
    private ArchiveSettingsRepository archiveSettingsRepository;

    @Mock
    private OfficerRepository officerRepository;

    @InjectMocks
    private OfficerScheduler scheduler;

    @Nested
    @DisplayName("Officer Trash Purge Tests")
    class OfficerTrashPurgeTests {

        @Test
        @DisplayName("givenAutoDeleteDisabled_whenAutoPurgeExpiredSoftDeletedOfficers_thenDoNothing")
        void givenAutoDeleteDisabled_whenAutoPurgeExpiredSoftDeletedOfficers_thenDoNothing() {
            // Given
            ArchiveSettings settings = new ArchiveSettings();
            settings.setAutoDeleteEnabled(false);
            given(archiveSettingsRepository.findById(1L)).willReturn(Optional.of(settings));

            // When
            scheduler.autoPurgeExpiredSoftDeletedOfficers();

            // Then
            then(officerRepository).should(never()).findExpiredSoftDeletedOfficers(any());
            then(officerRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("givenExpiredOfficers_whenAutoPurgeExpiredSoftDeletedOfficers_thenPermanentDeleteAndSave")
        void givenExpiredOfficers_whenAutoPurgeExpiredSoftDeletedOfficers_thenPermanentDeleteAndSave() {
            // Given
            ArchiveSettings settings = new ArchiveSettings();
            settings.setAutoDeleteEnabled(true);
            settings.setDeleteAfterDays(30);
            given(archiveSettingsRepository.findById(1L)).willReturn(Optional.of(settings));

            Officer officer = new Officer();
            officer.setId(10L);
            officer.setIsPermanentDeleted(false);
            given(officerRepository.findExpiredSoftDeletedOfficers(any(LocalDateTime.class)))
                    .willReturn(List.of(officer));

            // When
            scheduler.autoPurgeExpiredSoftDeletedOfficers();

            // Then
            assertThat(officer.getIsPermanentDeleted()).isTrue();
            then(officerRepository).should().save(officer);
        }
    }
}
