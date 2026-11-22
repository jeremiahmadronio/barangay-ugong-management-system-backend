package com.barangay.cases.vawc.service;

import com.barangay.cases.blotter.dto.ArchiveCaseRequest;
import com.barangay.cases.blotter.dto.BlotterLifecycleKpiResponse;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.projection.BlotterLifecycleKpiProjection;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.zshared.enumerated.CaseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * BDD-style unit tests for {@link VawcArchiveService}.
 */
@ExtendWith(MockitoExtension.class)
class VawcArchiveServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseTimelineService caseTimelineService;

    @InjectMocks
    private VawcArchiveService vawcArchiveService;

    @Nested
    @DisplayName("getVawcLifecycleKpi Tests")
    class GetVawcLifecycleKpiTests {

        /**
         * Verifies that VAWC lifecycle KPI metrics are properly mapped from projection.
         */
        @Test
        @DisplayName("given valid projection when getVawcLifecycleKpi then return mapped KPI response")
        void givenValidProjection_whenGetVawcLifecycleKpi_thenReturnMappedResponse() {
            // Given
            BlotterLifecycleKpiProjection projection = new BlotterLifecycleKpiProjection() {
                @Override
                public Number getTotalArchived() {
                    return 12L;
                }

                @Override
                public Number getTotalArchivedThisMonth() {
                    return 3L;
                }

                @Override
                public Number getTotalRecentlyDeleted() {
                    return 4L;
                }

                @Override
                public Number getTotalRecentlyDeletedThisMonth() {
                    return 1L;
                }
            };

            given(caseRepository.getVawcLifecycleKpiMetrics(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(projection);

            // When
            BlotterLifecycleKpiResponse response = vawcArchiveService.getVawcLifecycleKpi();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalArchived()).isEqualTo(12L);
            assertThat(response.totalArchivedThisMonth()).isEqualTo(3L);
            assertThat(response.totalRecentlyDeleted()).isEqualTo(4L);
            assertThat(response.totalRecentlyDeletedThisMonth()).isEqualTo(1L);

            then(caseRepository).should().getVawcLifecycleKpiMetrics(any(LocalDateTime.class), any(LocalDateTime.class));
        }

        /**
         * Verifies that null projection returns zeros gracefully.
         */
        @Test
        @DisplayName("given null projection when getVawcLifecycleKpi then return all zeros")
        void givenNullProjection_whenGetVawcLifecycleKpi_thenReturnZeros() {
            // Given
            given(caseRepository.getVawcLifecycleKpiMetrics(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(null);

            // When
            BlotterLifecycleKpiResponse response = vawcArchiveService.getVawcLifecycleKpi();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalArchived()).isZero();
            assertThat(response.totalArchivedThisMonth()).isZero();
            assertThat(response.totalRecentlyDeleted()).isZero();
            assertThat(response.totalRecentlyDeletedThisMonth()).isZero();
        }
    }

    @Nested
    @DisplayName("archiveCase Tests")
    class ArchiveCaseTests {

        /**
         * Verifies that closed VAWC case is archived with timestamp and remarks.
         */
        @Test
        @DisplayName("given closed case when archiveCase then set archived flags and save")
        void givenClosedCase_whenArchiveCase_thenSetArchivedFlags() {
            // Given
            Long caseId = 100L;
            Case testCase = new Case();
            testCase.setId(caseId);
            testCase.setCaseNumber("2026-VAWC-001");
            testCase.setCaseStatus(CaseStatus.DISMISSED);
            testCase.setIsArchived(false);
            testCase.setIsDeleted(false);

            given(caseRepository.findById(caseId)).willReturn(Optional.of(testCase));

            ArchiveCaseRequest request = new ArchiveCaseRequest("Resolved amicably");

            // When
            vawcArchiveService.archiveCase(caseId, request);

            // Then
            assertThat(testCase.getIsArchived()).isTrue();
            assertThat(testCase.getArchivedAt()).isNotNull();
            assertThat(testCase.getArchiveRemarks()).isEqualTo("Resolved amicably");

            then(caseRepository).should().saveAll(any());
        }

        /**
         * Verifies that ongoing/pending case throws IllegalStateException when archiving.
         */
        @Test
        @DisplayName("given pending active case when archiveCase then throw IllegalStateException")
        void givenActiveCase_whenArchiveCase_thenThrowIllegalStateException() {
            // Given
            Long caseId = 101L;
            Case testCase = new Case();
            testCase.setId(caseId);
            testCase.setCaseNumber("2026-VAWC-002");
            testCase.setCaseStatus(CaseStatus.PENDING);
            testCase.setIsArchived(false);
            testCase.setIsDeleted(false);

            given(caseRepository.findById(caseId)).willReturn(Optional.of(testCase));

            ArchiveCaseRequest request = new ArchiveCaseRequest("Archiving pending");

            // When & Then
            assertThatThrownBy(() -> vawcArchiveService.archiveCase(caseId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ongoing VAWC case");
        }

        /**
         * Verifies NoSuchElementException when case ID does not exist.
         */
        @Test
        @DisplayName("given non-existent case when archiveCase then throw NoSuchElementException")
        void givenNonExistentCase_whenArchiveCase_thenThrowNoSuchElementException() {
            // Given
            Long caseId = 999L;
            given(caseRepository.findById(caseId)).willReturn(Optional.empty());

            ArchiveCaseRequest request = new ArchiveCaseRequest("Remarks");

            // When & Then
            assertThatThrownBy(() -> vawcArchiveService.archiveCase(caseId, request))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("restoreArchivedCase Tests")
    class RestoreArchivedCaseTests {

        /**
         * Verifies restoring an archived case back to active status.
         */
        @Test
        @DisplayName("given archived case when restoreArchivedCase then reset archive flags")
        void givenArchivedCase_whenRestoreArchivedCase_thenResetArchiveFlags() {
            // Given
            Long caseId = 200L;
            Case testCase = new Case();
            testCase.setId(caseId);
            testCase.setCaseNumber("2026-VAWC-003");
            testCase.setIsArchived(true);
            testCase.setArchivedAt(LocalDateTime.now());
            testCase.setArchiveRemarks("Archived reason");

            given(caseRepository.findById(caseId)).willReturn(Optional.of(testCase));

            // When
            vawcArchiveService.restoreArchivedCase(caseId);

            // Then
            assertThat(testCase.getIsArchived()).isFalse();
            assertThat(testCase.getArchivedAt()).isNull();
            assertThat(testCase.getArchiveRemarks()).isNull();

            then(caseRepository).should().saveAll(any());
        }
    }

    @Nested
    @DisplayName("softDelete Tests")
    class SoftDeleteTests {

        /**
         * Verifies soft-deleting a closed case moves it to trash.
         */
        @Test
        @DisplayName("given closed case when softDelete then mark as deleted with timestamp")
        void givenClosedCase_whenSoftDelete_thenMarkDeleted() {
            // Given
            Long caseId = 300L;
            Case testCase = new Case();
            testCase.setId(caseId);
            testCase.setCaseNumber("2026-VAWC-004");
            testCase.setCaseStatus(CaseStatus.DISMISSED);
            testCase.setIsDeleted(false);

            given(caseRepository.findById(caseId)).willReturn(Optional.of(testCase));

            // When
            vawcArchiveService.softDelete(caseId);

            // Then
            assertThat(testCase.getIsDeleted()).isTrue();
            assertThat(testCase.getDeletedAt()).isNotNull();

            then(caseRepository).should().saveAll(any());
        }

        /**
         * Verifies that active case cannot be soft-deleted.
         */
        @Test
        @DisplayName("given active pending case when softDelete then throw IllegalStateException")
        void givenActiveCase_whenSoftDelete_thenThrowIllegalStateException() {
            // Given
            Long caseId = 301L;
            Case testCase = new Case();
            testCase.setId(caseId);
            testCase.setCaseNumber("2026-VAWC-005");
            testCase.setCaseStatus(CaseStatus.PENDING);
            testCase.setIsDeleted(false);

            given(caseRepository.findById(caseId)).willReturn(Optional.of(testCase));

            // When & Then
            assertThatThrownBy(() -> vawcArchiveService.softDelete(caseId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot move an ongoing VAWC case to trash");
        }
    }
}
