package com.barangay.cases.blotter.service;

import com.barangay.cases.blotter.dto.*;
import com.barangay.cases.shared.projection.BlotterDashboardKpiProjection;
import com.barangay.cases.shared.projection.SettlementMetricsProjection;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.repository.CaseSessionRepository;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link BlotterReportService}.
 */
@ExtendWith(MockitoExtension.class)
class BlotterReportServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseSessionRepository caseSessionRepository;

    @InjectMocks
    private BlotterReportService blotterReportService;

    @Nested
    @DisplayName("getSettlementEfficiency Method Tests")
    class GetSettlementEfficiencyTests {

        /**
         * Verifies computation of settlement efficiency and resolution rate metrics.
         */
        @Test
        @DisplayName("shouldComputeSettlementEfficiency_whenMetricsPresent")
        void shouldComputeSettlementEfficiency_whenMetricsPresent() {
            // Given
            LocalDate start = LocalDate.now().minusMonths(1);
            LocalDate end = LocalDate.now();

            SettlementMetricsProjection proj = mock(SettlementMetricsProjection.class);
            given(proj.getTotalCases()).willReturn(15L);
            given(proj.getTotalFormalComplaints()).willReturn(10L);
            given(proj.getTotalSettledCases()).willReturn(8L);

            given(caseRepository.getSettlementEfficiencyMetrics(
                    any(LocalDateTime.class),
                    any(LocalDateTime.class),
                    eq(1L),
                    eq(FilingType.FORMAL_COMPLAINT),
                    eq(CaseStatus.SETTLED)
            )).willReturn(proj);

            // When
            SettlementEfficiencyResponse response = blotterReportService.getSettlementEfficiency(start, end, 1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalCases()).isEqualTo(15L);
            assertThat(response.totalFormalComplaints()).isEqualTo(10L);
            assertThat(response.totalSettledCases()).isEqualTo(8L);
            assertThat(response.settlementRate()).isEqualTo(80.0);
        }
    }

    @Nested
    @DisplayName("getDashboardKpis Method Tests")
    class GetDashboardKpisTests {

        /**
         * Verifies real-time blotter operational dashboard metrics aggregation.
         */
        @Test
        @DisplayName("shouldComputeDashboardKpis_whenDataExists")
        void shouldComputeDashboardKpis_whenDataExists() {
            // Given
            BlotterDashboardKpiProjection proj = mock(BlotterDashboardKpiProjection.class);
            given(proj.getNewCasesThisWeek()).willReturn(5L);
            given(proj.getPendingCases()).willReturn(3L);
            given(proj.getUnresolvedCases()).willReturn(6L);

            given(caseRepository.getBlotterDashboardCaseKpis(any(LocalDateTime.class), any(LocalDateTime.class), any(), any()))
                    .willReturn(proj);
            given(caseSessionRepository.countUpcomingMediationsThisWeek(any(), any(), any(), any()))
                    .willReturn(4L);

            // When
            BlotterDashboardKpiResponse response = blotterReportService.getDashboardKpis();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalNewCasesThisWeek()).isEqualTo(5L);
            assertThat(response.totalPendingCases()).isEqualTo(3L);
            assertThat(response.totalUnresolvedCases()).isEqualTo(6L);
            assertThat(response.upcomingMediationsThisWeek()).isEqualTo(4L);
        }
    }
}
