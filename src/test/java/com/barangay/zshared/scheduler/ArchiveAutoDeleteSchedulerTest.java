package com.barangay.zshared.scheduler;

import com.barangay.cases.blotter.service.BlotterArchiveAndDeleteService;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.entity.ArchiveSettings;
import com.barangay.zshared.enumerated.OfficerStatus;
import com.barangay.zshared.enumerated.ResidentStatus;
import com.barangay.zshared.enumerated.UserStatus;
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
 * BDD Unit tests for {@link ArchiveAutoDeleteScheduler}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ArchiveAutoDeleteScheduler BDD Tests")
class ArchiveAutoDeleteSchedulerTest {

    @Mock
    private ArchiveSettingsRepository archiveSettingsRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private OfficerRepository officerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private BlotterArchiveAndDeleteService blotterArchiveAndDeleteService;

    @InjectMocks
    private ArchiveAutoDeleteScheduler scheduler;

    @Nested
    @DisplayName("Auto-delete Archives Tests")
    class AutoDeleteArchivesTests {

        @Test
        @DisplayName("givenAutoDeleteDisabled_whenAutoDeleteExpiredArchives_thenDoNothing")
        void givenAutoDeleteDisabled_whenAutoDeleteExpiredArchives_thenDoNothing() {
            // Given
            ArchiveSettings disabledSettings = new ArchiveSettings();
            disabledSettings.setAutoDeleteEnabled(false);
            given(archiveSettingsRepository.findById(1L)).willReturn(Optional.of(disabledSettings));

            // When
            scheduler.autoDeleteExpiredArchives();

            // Then
            then(caseRepository).should(never()).findExpiredSoftDeletedCases(any());
            then(userRepository).should(never()).findAllByStatus(any());
            then(officerRepository).should(never()).findAllByStatus(any());
            then(residentRepository).should(never()).findAllByStatus(any());
        }

        @Test
        @DisplayName("givenAutoDeleteEnabledAndExpiredRecords_whenAutoDeleteExpiredArchives_thenPermanentlyPurgeAll")
        void givenAutoDeleteEnabledAndExpiredRecords_whenAutoDeleteExpiredArchives_thenPermanentlyPurgeAll() {
            // Given
            ArchiveSettings enabledSettings = new ArchiveSettings();
            enabledSettings.setAutoDeleteEnabled(true);
            enabledSettings.setDeleteAfterDays(30);
            given(archiveSettingsRepository.findById(1L)).willReturn(Optional.of(enabledSettings));

            Case mockCase = new Case();
            mockCase.setId(10L);
            mockCase.setCaseNumber("CASE-2026-001");
            given(caseRepository.findExpiredSoftDeletedCases(any(LocalDateTime.class))).willReturn(List.of(mockCase));

            LocalDateTime expiredDate = LocalDateTime.now().minusDays(40);

            User mockUser = new User();
            mockUser.setId(20L);
            mockUser.setUsername("testuser");
            mockUser.setArchivedAt(expiredDate);
            mockUser.setIsPermanentDeleted(false);
            given(userRepository.findAllByStatus(UserStatus.ARCHIVED)).willReturn(List.of(mockUser));

            Officer mockOfficer = new Officer();
            mockOfficer.setId(30L);
            mockOfficer.setUpdatedAt(expiredDate);
            mockOfficer.setIsPermanentDeleted(false);
            given(officerRepository.findAllByStatus(OfficerStatus.ARCHIVED)).willReturn(List.of(mockOfficer));

            Resident mockResident = new Resident();
            mockResident.setResidentId(40L);
            mockResident.setUpdatedAt(expiredDate);
            mockResident.setIsPermanentDeleted(false);
            given(residentRepository.findAllByStatus(ResidentStatus.ARCHIVED)).willReturn(List.of(mockResident));

            // When
            scheduler.autoDeleteExpiredArchives();

            // Then
            then(blotterArchiveAndDeleteService).should().permanentDeleteCaseByFlag(10L);
            then(userRepository).should().save(mockUser);
            assertThat(mockUser.getIsPermanentDeleted()).isTrue();

            then(officerRepository).should().save(mockOfficer);
            assertThat(mockOfficer.getIsPermanentDeleted()).isTrue();

            then(residentRepository).should().save(mockResident);
            assertThat(mockResident.getIsPermanentDeleted()).isTrue();
        }
    }
}
