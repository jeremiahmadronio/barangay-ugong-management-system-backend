package com.barangay.cases.bcpc.service;

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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * BDD Unit tests for {@link BcpcArchiveService}.
 * Tests lifecycle metrics and case archival rules for BCPC records.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BcpcArchiveService BDD Tests")
class BcpcArchiveServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseTimelineService caseTimelineService;

    @InjectMocks
    private BcpcArchiveService bcpcArchiveService;

    @Nested
    @DisplayName("Lifecycle KPI Tests")
    class LifecycleKpiTests {

        @Test
        @DisplayName("givenActiveCases_whenGetBcpcLifecycleKpi_thenReturnAggregatedMetrics")
        void givenActiveCases_whenGetBcpcLifecycleKpi_thenReturnAggregatedMetrics() {
            // Given
            BlotterLifecycleKpiProjection projection = mock(BlotterLifecycleKpiProjection.class);
            given(projection.getTotalArchived()).willReturn(15L);
            given(projection.getTotalArchivedThisMonth()).willReturn(3L);
            given(projection.getTotalRecentlyDeleted()).willReturn(2L);
            given(projection.getTotalRecentlyDeletedThisMonth()).willReturn(1L);

            given(caseRepository.getBcpcLifecycleKpiMetrics(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(projection);

            // When
            BlotterLifecycleKpiResponse response = bcpcArchiveService.getBcpcLifecycleKpi();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalArchived()).isEqualTo(15L);
            assertThat(response.totalArchivedThisMonth()).isEqualTo(3L);
            assertThat(response.totalRecentlyDeleted()).isEqualTo(2L);
            assertThat(response.totalRecentlyDeletedThisMonth()).isEqualTo(1L);
            then(caseRepository).should().getBcpcLifecycleKpiMetrics(any(LocalDateTime.class), any(LocalDateTime.class));
        }
    }

    @Nested
    @DisplayName("Archive and Restore Tests")
    class ArchiveAndRestoreTests {

        @Test
        @DisplayName("givenOngoingCase_whenArchiveCase_thenThrowIllegalStateException")
        void givenOngoingCase_whenArchiveCase_thenThrowIllegalStateException() {
            // Given
            Case aCase = new Case();
            aCase.setId(10L);
            aCase.setCaseStatus(CaseStatus.ONGOING);
            ArchiveCaseRequest request = new ArchiveCaseRequest("Settled already");

            given(caseRepository.findById(10L)).willReturn(Optional.of(aCase));

            // When & Then
            assertThatThrownBy(() -> bcpcArchiveService.archiveCase(10L, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot archive an active BCPC case");
        }

        @Test
        @DisplayName("givenSettledCase_whenArchiveCase_thenSuccessfullyArchive")
        void givenSettledCase_whenArchiveCase_thenSuccessfullyArchive() {
            // Given
            Case aCase = new Case();
            aCase.setId(20L);
            aCase.setCaseNumber("BU26-BCPC-0005");
            aCase.setCaseStatus(CaseStatus.SETTLED);
            aCase.setIsArchived(false);
            ArchiveCaseRequest request = new ArchiveCaseRequest("Archiving settled case");

            given(caseRepository.findById(20L)).willReturn(Optional.of(aCase));
            given(caseRepository.saveAll(any())).willAnswer(invocation -> invocation.getArgument(0));

            // When
            bcpcArchiveService.archiveCase(20L, request);

            // Then
            assertThat(aCase.getIsArchived()).isTrue();
            then(caseRepository).should().saveAll(any());
        }
    }
}
