package com.barangay.zshared.scheduler;

import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
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
 * BDD Unit tests for {@link UserScheduler}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserScheduler BDD Tests")
class UserSchedulerTest {

    @Mock
    private ArchiveSettingsRepository archiveSettingsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserScheduler scheduler;

    @Nested
    @DisplayName("User Trash Purge Tests")
    class UserTrashPurgeTests {

        @Test
        @DisplayName("givenAutoDeleteDisabled_whenAutoPurgeExpiredSoftDeletedUsers_thenDoNothing")
        void givenAutoDeleteDisabled_whenAutoPurgeExpiredSoftDeletedUsers_thenDoNothing() {
            // Given
            ArchiveSettings settings = new ArchiveSettings();
            settings.setAutoDeleteEnabled(false);
            given(archiveSettingsRepository.findById(1L)).willReturn(Optional.of(settings));

            // When
            scheduler.autoPurgeExpiredSoftDeletedUsers();

            // Then
            then(userRepository).should(never()).findExpiredSoftDeletedUsers(any());
            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("givenExpiredUsers_whenAutoPurgeExpiredSoftDeletedUsers_thenPermanentDeleteAndLockAndSave")
        void givenExpiredUsers_whenAutoPurgeExpiredSoftDeletedUsers_thenPermanentDeleteAndLockAndSave() {
            // Given
            ArchiveSettings settings = new ArchiveSettings();
            settings.setAutoDeleteEnabled(true);
            settings.setDeleteAfterDays(30);
            given(archiveSettingsRepository.findById(1L)).willReturn(Optional.of(settings));

            User user = new User();
            user.setId(77L);
            user.setUsername("john_doe");
            user.setIsPermanentDeleted(false);
            user.setIsLocked(false);
            given(userRepository.findExpiredSoftDeletedUsers(any(LocalDateTime.class)))
                    .willReturn(List.of(user));

            // When
            scheduler.autoPurgeExpiredSoftDeletedUsers();

            // Then
            assertThat(user.getIsPermanentDeleted()).isTrue();
            assertThat(user.getIsLocked()).isTrue();
            then(userRepository).should().save(user);
        }
    }
}
