package com.barangay.cases.lupon.service;

import com.barangay.cases.blotter.dto.ArchiveCaseRequest;
import com.barangay.cases.blotter.dto.BlotterLifecycleKpiResponse;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.projection.BlotterLifecycleKpiProjection;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.zshared.enumerated.CaseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * BDD-style unit tests for {@link LuponArchive}.
 */
@ExtendWith(MockitoExtension.class)
class LuponArchiveTest {

    @Mock
    private CaseRepository caseRepository;

    @InjectMocks
    private LuponArchive luponArchive;

    @Nested
    @DisplayName("getLuponLifecycleKpi Tests")
    class GetLuponLifecycleKpiTests {

        /**
         * Verifies that KPI metrics are correctly mapped from projection.
         */
        @Test
        @DisplayName("given valid projection when getLuponLifecycleKpi then return mapped KPI response")
        void givenValidProjection_whenGetLuponLifecycleKpi_thenReturnMappedResponse() {
            // Given
            BlotterLifecycleKpiProjection projection = new BlotterLifecycleKpiProjection() {
                @Override
                public Number getTotalArchived() {
                    return 10L;
                }

                @Override
                public Number getTotalArchivedThisMonth() {
                    return 2L;
                }

                @Override
                public Number getTotalRecentlyDeleted() {
                    return 5L;
                }

                @Override
                public Number getTotalRecentlyDeletedThisMonth() {
                    return 1L;
                }
            };
            given(caseRepository.getLuponLifecycleKpiMetrics(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(projection);

            // When
            BlotterLifecycleKpiResponse response = luponArchive.getLuponLifecycleKpi();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalArchived()).isEqualTo(10L);
            assertThat(response.totalArchivedThisMonth()).isEqualTo(2L);
            assertThat(response.totalRecentlyDeleted()).isEqualTo(5L);
            assertThat(response.totalRecentlyDeletedThisMonth()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("archiveCase Tests")
    class ArchiveCaseTests {

        /**
         * Verifies successful archiving of a settled case.
         */
        @Test
        @DisplayName("given settled case when archiveCase then update case flags and save")
        void givenSettledCase_whenArchiveCase_thenUpdateFlagsAndSave() {
            // Given
            Long caseId = 1L;
            Case targetCase = new Case();
            targetCase.setId(caseId);
            targetCase.setCaseNumber("LUP-2026-001");
            targetCase.setCaseStatus(CaseStatus.SETTLED);
            targetCase.setIsArchived(false);
            targetCase.setIsDeleted(false);

            given(caseRepository.findById(caseId)).willReturn(Optional.of(targetCase));

            ArchiveCaseRequest request = new ArchiveCaseRequest("Settled at conciliation");

            // When
            luponArchive.archiveCase(caseId, request);

            // Then
            assertThat(targetCase.getIsArchived()).isTrue();
            assertThat(targetCase.getArchiveRemarks()).isEqualTo("Settled at conciliation");
            then(caseRepository).should().saveAll(List.of(targetCase));
        }

        /**
         * Verifies exception when archiving an ongoing conciliation case.
         */
        @Test
        @DisplayName("given active conciliation case when archiveCase then throw IllegalStateException")
        void givenActiveConciliationCase_whenArchiveCase_thenThrowIllegalStateException() {
            // Given
            Long caseId = 1L;
            Case targetCase = new Case();
            targetCase.setId(caseId);
            targetCase.setCaseNumber("LUP-2026-001");
            targetCase.setCaseStatus(CaseStatus.UNDER_CONCILIATION);
            targetCase.setIsArchived(false);
            targetCase.setIsDeleted(false);

            given(caseRepository.findById(caseId)).willReturn(Optional.of(targetCase));

            // When & Then
            assertThatThrownBy(() -> luponArchive.archiveCase(caseId, new ArchiveCaseRequest("remarks")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot archive an ongoing conciliation case");
        }

        /**
         * Verifies exception when case is not found.
         */
        @Test
        @DisplayName("given non-existent case when archiveCase then throw NoSuchElementException")
        void givenNonExistentCase_whenArchiveCase_thenThrowNoSuchElementException() {
            // Given
            Long caseId = 999L;
            given(caseRepository.findById(caseId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> luponArchive.archiveCase(caseId, new ArchiveCaseRequest("remarks")))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("restoreArchivedCase Tests")
    class RestoreArchivedCaseTests {

        /**
         * Verifies un-archiving of an archived case.
         */
        @Test
        @DisplayName("given archived case when restoreArchivedCase then clear archive flags and save")
        void givenArchivedCase_whenRestoreArchivedCase_thenClearFlagsAndSave() {
            // Given
            Long caseId = 1L;
            Case targetCase = new Case();
            targetCase.setId(caseId);
            targetCase.setCaseNumber("LUP-2026-001");
            targetCase.setIsArchived(true);
            targetCase.setArchiveRemarks("Past archive");
            targetCase.setArchivedAt(LocalDateTime.now());

            given(caseRepository.findById(caseId)).willReturn(Optional.of(targetCase));

            // When
            luponArchive.restoreArchivedCase(caseId);

            // Then
            assertThat(targetCase.getIsArchived()).isFalse();
            assertThat(targetCase.getArchiveRemarks()).isNull();
            assertThat(targetCase.getArchivedAt()).isNull();
            then(caseRepository).should().saveAll(List.of(targetCase));
        }
    }

    @Nested
    @DisplayName("softDelete Tests")
    class SoftDeleteTests {

        /**
         * Verifies moving a settled case to the trash vault.
         */
        @Test
        @DisplayName("given settled case when softDelete then mark as deleted and save")
        void givenSettledCase_whenSoftDelete_thenMarkDeletedAndSave() {
            // Given
            Long caseId = 1L;
            Case targetCase = new Case();
            targetCase.setId(caseId);
            targetCase.setCaseNumber("LUP-2026-001");
            targetCase.setCaseStatus(CaseStatus.SETTLED);
            targetCase.setIsDeleted(false);

            given(caseRepository.findById(caseId)).willReturn(Optional.of(targetCase));

            // When
            luponArchive.softDelete(caseId);

            // Then
            assertThat(targetCase.getIsDeleted()).isTrue();
            then(caseRepository).should().saveAll(List.of(targetCase));
        }

        /**
         * Verifies exception when trying to delete an ongoing conciliation case.
         */
        @Test
        @DisplayName("given active conciliation case when softDelete then throw IllegalStateException")
        void givenActiveConciliationCase_whenSoftDelete_thenThrowIllegalStateException() {
            // Given
            Long caseId = 1L;
            Case targetCase = new Case();
            targetCase.setId(caseId);
            targetCase.setCaseNumber("LUP-2026-001");
            targetCase.setCaseStatus(CaseStatus.UNDER_CONCILIATION);
            targetCase.setIsDeleted(false);

            given(caseRepository.findById(caseId)).willReturn(Optional.of(targetCase));

            // When & Then
            assertThatThrownBy(() -> luponArchive.softDelete(caseId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot move an ongoing conciliation case to trash");
        }
    }
}
