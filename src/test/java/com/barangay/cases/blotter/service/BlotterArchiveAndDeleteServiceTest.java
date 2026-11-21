package com.barangay.cases.blotter.service;

import com.barangay.cases.blotter.dto.BlotterArchiveFilterRequest;
import com.barangay.cases.blotter.dto.BlotterArchiveItemResponse;
import com.barangay.cases.blotter.dto.BlotterLifecycleKpiResponse;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.projection.BlotterLifecycleKpiProjection;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import com.barangay.zshared.mapper.PageResponse;
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
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link BlotterArchiveAndDeleteService}.
 */
@ExtendWith(MockitoExtension.class)
class BlotterArchiveAndDeleteServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @InjectMocks
    private BlotterArchiveAndDeleteService blotterArchiveAndDeleteService;

    @Nested
    @DisplayName("getBlotterLifecycleKpi Method Tests")
    class GetBlotterLifecycleKpiTests {

        /**
         * Verifies calculation of Blotter lifecycle KPIs from repository metrics projection.
         */
        @Test
        @DisplayName("shouldReturnLifecycleKpis_whenMetricsAvailable")
        void shouldReturnLifecycleKpis_whenMetricsAvailable() {
            // Given
            BlotterLifecycleKpiProjection metrics = mock(BlotterLifecycleKpiProjection.class);
            given(metrics.getTotalArchived()).willReturn(12L);
            given(metrics.getTotalArchivedThisMonth()).willReturn(4L);
            given(metrics.getTotalRecentlyDeleted()).willReturn(3L);
            given(metrics.getTotalRecentlyDeletedThisMonth()).willReturn(1L);

            given(caseRepository.getBlotterLifecycleKpiMetrics(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(metrics);

            // When
            BlotterLifecycleKpiResponse response = blotterArchiveAndDeleteService.getBlotterLifecycleKpi();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalArchived()).isEqualTo(12L);
            assertThat(response.totalArchivedThisMonth()).isEqualTo(4L);
            assertThat(response.totalRecentlyDeleted()).isEqualTo(3L);
            assertThat(response.totalRecentlyDeletedThisMonth()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("getArchivedBlotterCases Method Tests")
    class GetArchivedBlotterCasesTests {

        /**
         * Verifies retrieval of paginated archived blotter cases.
         */
        @Test
        @DisplayName("shouldReturnPaginatedArchivedCases_whenFilterProvided")
        void shouldReturnPaginatedArchivedCases_whenFilterProvided() {
            // Given
            BlotterArchiveFilterRequest filter = new BlotterArchiveFilterRequest(
                    "BLTR", "FORMAL_COMPLAINT", 0, 10
            );

            BlotterArchiveItemResponse item = new BlotterArchiveItemResponse(
                    1L,
                    "BLTR-2026-0001",
                    "Juan Dela Cruz",
                    "Pedro Santos",
                    "Dispute settled",
                    CaseStatus.SETTLED,
                    LocalDateTime.now(),
                    FilingType.FORMAL_COMPLAINT
            );

            Page<BlotterArchiveItemResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);
            given(caseRepository.findArchivedBlotterCases(any(BlotterArchiveFilterRequest.class), any(Pageable.class)))
                    .willReturn(page);

            // When
            PageResponse<BlotterArchiveItemResponse> response = blotterArchiveAndDeleteService.getArchivedBlotterCases(filter);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).caseNumber()).isEqualTo("BLTR-2026-0001");
            assertThat(response.getTotalElements()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("archiveCase Method Tests")
    class ArchiveCaseTests {

        /**
         * Verifies archiving a resolved case successfully.
         */
        @Test
        @DisplayName("shouldArchiveCase_whenCaseIsResolved")
        void shouldArchiveCase_whenCaseIsResolved() {
            // Given
            Long caseId = 5L;
            Case rootCase = new Case();
            rootCase.setId(caseId);
            rootCase.setCaseNumber("BLTR-2026-0005");
            rootCase.setCaseStatus(CaseStatus.SETTLED);
            rootCase.setIsDeleted(false);
            rootCase.setIsArchived(false);

            given(caseRepository.findById(caseId)).willReturn(Optional.of(rootCase));
            given(caseRepository.saveAll(any())).willReturn(List.of(rootCase));

            // When
            blotterArchiveAndDeleteService.archiveCase(caseId, null);

            // Then
            assertThat(rootCase.getIsArchived()).isTrue();
            assertThat(rootCase.getArchivedAt()).isNotNull();
            then(caseRepository).should().saveAll(any());
        }

        /**
         * Verifies IllegalStateException is thrown when attempting to archive an active/ongoing case.
         */
        @Test
        @DisplayName("shouldThrowIllegalStateException_whenArchivingActiveCase")
        void shouldThrowIllegalStateException_whenArchivingActiveCase() {
            // Given
            Long caseId = 6L;
            Case activeCase = new Case();
            activeCase.setId(caseId);
            activeCase.setCaseNumber("BLTR-2026-0006");
            activeCase.setCaseStatus(CaseStatus.PENDING);
            activeCase.setIsDeleted(false);
            activeCase.setIsArchived(false);

            given(caseRepository.findById(caseId)).willReturn(Optional.of(activeCase));

            // When & Then
            assertThatThrownBy(() -> blotterArchiveAndDeleteService.archiveCase(caseId, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot archive an ongoing case");
        }
    }
}
