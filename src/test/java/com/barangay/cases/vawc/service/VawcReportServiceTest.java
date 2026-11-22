package com.barangay.cases.vawc.service;

import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.vawc.dto.VawcDashboardKpiResponse;
import com.barangay.cases.vawc.dto.VawcReportKpiResponse;
import com.barangay.cases.vawc.repository.BarangayProtectionOrderRepository;
import com.barangay.cases.vawc.repository.ReferralFeedbackRepository;
import com.barangay.cases.vawc.repository.VawcReferralRepository;
import com.barangay.zshared.enumerated.CaseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * BDD-style unit tests for {@link VawcReportService}.
 */
@ExtendWith(MockitoExtension.class)
class VawcReportServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private VawcReferralRepository vawcReferralRepository;

    @Mock
    private ReferralFeedbackRepository referralFeedbackRepository;

    @Mock
    private BarangayProtectionOrderRepository barangayProtectionOrderRepository;

    @Mock
    private VawcCaseService vawcCaseService;

    @InjectMocks
    private VawcReportService vawcReportService;

    @Nested
    @DisplayName("getDashboardKpis Tests")
    class GetDashboardKpisTests {

        /**
         * Verifies real-time dashboard KPIs calculation for weekly metrics.
         */
        @Test
        @DisplayName("given weekly counts when getDashboardKpis then return mapped dashboard response")
        void givenWeeklyCounts_whenGetDashboardKpis_thenReturnMappedResponse() {
            // Given
            given(caseRepository.countRootVawcCasesByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(8L, 4L); // this week = 8, prev week = 4 (+100.0%)

            given(barangayProtectionOrderRepository.countActiveBpoBetween(
                    any(Collection.class),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)
            )).willReturn(5L);

            given(vawcReferralRepository.countReferralsBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(3L);

            given(caseRepository.countVawcCasesByCaseStatusInBetween(
                    eq(List.of(CaseStatus.WITHDRAWN)),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)
            )).willReturn(1L);

            // When
            VawcDashboardKpiResponse response = vawcReportService.getDashboardKpis();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalCasesThisWeek()).isEqualTo(8L);
            assertThat(response.totalCasesTrend()).isEqualTo("+100.0% vs previous period");
            assertThat(response.totalActiveBpoThisWeek()).isEqualTo(5L);
            assertThat(response.totalReferredCasesThisWeek()).isEqualTo(3L);
            assertThat(response.totalWithdrawnCasesThisWeek()).isEqualTo(1L);

            then(caseRepository).should().countVawcCasesByCaseStatusInBetween(
                    eq(List.of(CaseStatus.WITHDRAWN)),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)
            );
        }
    }

    @Nested
    @DisplayName("getReportKpi Tests")
    class GetReportKpiTests {

        /**
         * Verifies report KPIs calculation within a specified date range.
         */
        @Test
        @DisplayName("given date range when getReportKpi then return KPI metrics")
        void givenDateRange_whenGetReportKpi_thenReturnReportKpiResponse() {
            // Given
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 3, 31);

            given(caseRepository.countRootVawcCasesByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(20L, 10L);

            given(vawcReferralRepository.countReferralsBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(7L, 5L);

            given(caseRepository.countVawcCasesByCaseStatusInBetween(
                    eq(List.of(CaseStatus.DISMISSED)),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)
            )).willReturn(2L, 1L);

            given(vawcReferralRepository.findReferralsBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(Collections.emptyList());

            // When
            VawcReportKpiResponse response = vawcReportService.getReportKpi(startDate, endDate);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalCases()).isEqualTo(20L);
            assertThat(response.totalReferredCases()).isEqualTo(7L);
            assertThat(response.totalDismissedCases()).isEqualTo(2L);
            assertThat(response.mostReferredAgency()).isEqualTo("None Yet");

            then(caseRepository).should(times(2)).countVawcCasesByCaseStatusInBetween(
                    eq(List.of(CaseStatus.DISMISSED)),
                    any(LocalDateTime.class),
                    any(LocalDateTime.class)
            );
        }
    }
}
