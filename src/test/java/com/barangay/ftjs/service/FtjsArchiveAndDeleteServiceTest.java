package com.barangay.ftjs.service;

import com.barangay.ftjs.dto.FtjsArchiveStatsResponse;
import com.barangay.ftjs.dto.FtjsSummaryResponse;
import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.ftjs.model.FirstTimeJobSeekerTimeLine;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerTimeLineRepository;
import com.barangay.ftjs.repository.FtjsSystemSettingRepository;
import com.barangay.zshared.enumerated.FtjsStatus;
import com.barangay.zshared.exception.ResourceNotFoundException;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FtjsArchiveAndDeleteServiceTest {

    @Mock
    private FirstTimeJobSeekerRepository ftjsRepository;

    @Mock
    private FtjsSystemSettingRepository ftjsSettingRepository;

    @Mock
    private FirstTimeJobSeekerTimeLineRepository ftjsTimeLineRepository;

    @InjectMocks
    private FtjsArchiveAndDeleteService archiveAndDeleteService;

    @Nested
    @DisplayName("archiveFtjs Method Tests")
    class ArchiveFtjsTests {

        @Test
        @DisplayName("shouldArchiveRecord_whenRecordExists")
        void shouldArchiveRecord_whenRecordExists() {
            // Given
            Long id = 1L;
            when(ftjsRepository.archiveRecordNative(id)).thenReturn(1);

            // When
            archiveAndDeleteService.archiveFtjs(id);

            // Then
            verify(ftjsRepository, times(1)).archiveRecordNative(id);
            verify(ftjsTimeLineRepository, times(1)).save(any(FirstTimeJobSeekerTimeLine.class));
        }

        @Test
        @DisplayName("shouldThrowResourceNotFoundException_whenRecordNotFoundForArchive")
        void shouldThrowResourceNotFoundException_whenRecordNotFoundForArchive() {
            // Given
            Long id = 999L;
            when(ftjsRepository.archiveRecordNative(id)).thenReturn(0);

            // When / Then
            assertThatThrownBy(() -> archiveAndDeleteService.archiveFtjs(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ftjs not found with ID: 999");
        }
    }

    @Nested
    @DisplayName("restoreFtjs Method Tests")
    class RestoreFtjsTests {

        @Test
        @DisplayName("shouldRestoreRecord_whenRecordExists")
        void shouldRestoreRecord_whenRecordExists() {
            // Given
            Long id = 1L;
            when(ftjsRepository.restoreRecordNative(id)).thenReturn(1);

            // When
            archiveAndDeleteService.restoreFtjs(id);

            // Then
            verify(ftjsRepository, times(1)).restoreRecordNative(id);
            verify(ftjsTimeLineRepository, times(1)).save(any(FirstTimeJobSeekerTimeLine.class));
        }

        @Test
        @DisplayName("shouldThrowResourceNotFoundException_whenRecordNotFoundForRestore")
        void shouldThrowResourceNotFoundException_whenRecordNotFoundForRestore() {
            // Given
            Long id = 888L;
            when(ftjsRepository.restoreRecordNative(id)).thenReturn(0);

            // When / Then
            assertThatThrownBy(() -> archiveAndDeleteService.restoreFtjs(id))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Ftjs not found with ID: 888");
        }
    }

    @Nested
    @DisplayName("getArchiveStats Method Tests")
    class GetArchiveStatsTests {

        @Test
        @DisplayName("shouldReturnArchiveStats_whenCalled")
        void shouldReturnArchiveStats_whenCalled() {
            // Given
            when(ftjsRepository.countByIsArchivedTrue()).thenReturn(15L);
            when(ftjsRepository.countArchiveBetween(any(), any())).thenReturn(5L).thenReturn(2L);
            when(ftjsRepository.countArchiveOriginalIssuance()).thenReturn(10L);
            when(ftjsRepository.countTotalRecentDelete()).thenReturn(3L);

            // When
            FtjsArchiveStatsResponse response = archiveAndDeleteService.getArchiveStats();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalArchived()).isEqualTo(15L);
            assertThat(response.totalArchivedThisMonth()).isEqualTo(5L);
            assertThat(response.totalArchiveOriginalIssuance()).isEqualTo(10L);
            assertThat(response.totalRecentlyDeleted()).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("permanentlyDelete Method Tests")
    class PermanentlyDeleteTests {

        @Test
        @DisplayName("shouldDeletePermanently_whenRecordExists")
        void shouldDeletePermanently_whenRecordExists() {
            // Given
            Long id = 5L;
            when(ftjsRepository.hardDeleteById(id)).thenReturn(1);

            // When
            archiveAndDeleteService.permanentlyDelete(id);

            // Then
            verify(ftjsRepository, times(1)).deleteChildAffidavits(id);
            verify(ftjsRepository, times(1)).deleteChildTimelines(id);
            verify(ftjsRepository, times(1)).deleteChildNotes(id);
            verify(ftjsRepository, times(1)).hardDeleteById(id);
        }

        @Test
        @DisplayName("shouldThrowEntityNotFoundException_whenRecordNotFoundInTrash")
        void shouldThrowEntityNotFoundException_whenRecordNotFoundInTrash() {
            // Given
            Long id = 404L;
            when(ftjsRepository.hardDeleteById(id)).thenReturn(0);

            // When / Then
            assertThatThrownBy(() -> archiveAndDeleteService.permanentlyDelete(id))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Deleted FTJS application with ID 404 not found in trash.");
        }
    }

    @Nested
    @DisplayName("softDelete Method Tests")
    class SoftDeleteTests {

        @Test
        @DisplayName("shouldSoftDeleteRecord_whenActiveRecordExists")
        void shouldSoftDeleteRecord_whenActiveRecordExists() {
            // Given
            Long id = 2L;
            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setId(id);
            ftjs.setFtjsNumber("2026-FTJS-0002");
            ftjs.setDeleted(false);

            when(ftjsRepository.findById(id)).thenReturn(Optional.of(ftjs));

            // When
            archiveAndDeleteService.softDelete(id);

            // Then
            assertThat(ftjs.isDeleted()).isTrue();
            assertThat(ftjs.getDeletedAt()).isNotNull();
            verify(ftjsRepository, times(1)).save(ftjs);
            verify(ftjsTimeLineRepository, times(1)).save(any(FirstTimeJobSeekerTimeLine.class));
        }

        @Test
        @DisplayName("shouldThrowEntityNotFoundException_whenActiveRecordNotFound")
        void shouldThrowEntityNotFoundException_whenActiveRecordNotFound() {
            // Given
            Long id = 555L;
            when(ftjsRepository.findById(id)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> archiveAndDeleteService.softDelete(id))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Active FTJS record with ID 555 not found.");
        }
    }
}
