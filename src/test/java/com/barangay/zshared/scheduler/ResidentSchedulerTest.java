package com.barangay.zshared.scheduler;

import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
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
 * BDD Unit tests for {@link ResidentScheduler}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResidentScheduler BDD Tests")
class ResidentSchedulerTest {

    @Mock
    private ArchiveSettingsRepository archiveSettingsRepository;

    @Mock
    private ResidentRepository residentRepository;

    @InjectMocks
    private ResidentScheduler scheduler;

    @Nested
    @DisplayName("Resident Trash Purge Tests")
    class ResidentTrashPurgeTests {

        @Test
        @DisplayName("givenAutoDeleteDisabled_whenAutoPurgeExpiredSoftDeletedResidents_thenDoNothing")
        void givenAutoDeleteDisabled_whenAutoPurgeExpiredSoftDeletedResidents_thenDoNothing() {
            // Given
            ArchiveSettings settings = new ArchiveSettings();
            settings.setAutoDeleteEnabled(false);
            given(archiveSettingsRepository.findById(1L)).willReturn(Optional.of(settings));

            // When
            scheduler.autoPurgeExpiredSoftDeletedResidents();

            // Then
            then(residentRepository).should(never()).findExpiredSoftDeletedResidents(any());
            then(residentRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("givenExpiredResidents_whenAutoPurgeExpiredSoftDeletedResidents_thenPermanentDeleteAndSave")
        void givenExpiredResidents_whenAutoPurgeExpiredSoftDeletedResidents_thenPermanentDeleteAndSave() {
            // Given
            ArchiveSettings settings = new ArchiveSettings();
            settings.setAutoDeleteEnabled(true);
            settings.setDeleteAfterDays(30);
            given(archiveSettingsRepository.findById(1L)).willReturn(Optional.of(settings));

            Resident resident = new Resident();
            resident.setResidentId(25L);
            resident.setIsPermanentDeleted(false);
            given(residentRepository.findExpiredSoftDeletedResidents(any(LocalDateTime.class)))
                    .willReturn(List.of(resident));

            // When
            scheduler.autoPurgeExpiredSoftDeletedResidents();

            // Then
            assertThat(resident.getIsPermanentDeleted()).isTrue();
            then(residentRepository).should().save(resident);
        }
    }
}
