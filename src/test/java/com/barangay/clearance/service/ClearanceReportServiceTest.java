package com.barangay.clearance.service;

import com.barangay.clearance.dto.ClearanceDailyTrendResponse;
import com.barangay.clearance.dto.ClearanceDashboardKpiResponse;
import com.barangay.clearance.model.CertificateTemplate;
import com.barangay.clearance.model.IssuedCertificate;
import com.barangay.clearance.projection.ClearanceDailyTrendProjection;
import com.barangay.clearance.projection.MostIssuedCertProjection;
import com.barangay.clearance.repository.IssuedCertificateRepository;
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
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Unit tests for {@link ClearanceReportService}.
 */
@ExtendWith(MockitoExtension.class)
class ClearanceReportServiceTest {

    @Mock
    private IssuedCertificateRepository issuedCertificateRepository;

    @InjectMocks
    private ClearanceReportService clearanceReportService;

    @Nested
    @DisplayName("getDashboardKpis Method Tests")
    class GetDashboardKpisTests {

        /**
         * Verifies calculation of weekly dashboard KPIs when data is present.
         */
        @Test
        @DisplayName("shouldComputeAccurateDashboardKpis_whenDataExists")
        void shouldComputeAccurateDashboardKpis_whenDataExists() {
            // Given
            given(issuedCertificateRepository.countActiveCertificatesBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(2L)  // Today count
                    .willReturn(5L); // Week count

            given(issuedCertificateRepository.countActiveCertificates()).willReturn(5L);

            MostIssuedCertProjection proj = mock(MostIssuedCertProjection.class);
            given(proj.getCertTitle()).willReturn("BARANGAY CLEARANCE");
            given(proj.getCount()).willReturn(4L);
            given(issuedCertificateRepository.getMostIssuedTemplate(any(LocalDateTime.class), any(LocalDateTime.class), eq(null)))
                    .willReturn(List.of(proj));

            // When
            ClearanceDashboardKpiResponse response = clearanceReportService.getDashboardKpis();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalIssuedToday()).isEqualTo(2L);
            assertThat(response.totalIssuedThisWeek()).isEqualTo(5L);
            assertThat(response.totalActiveClearances()).isEqualTo(5L);
            assertThat(response.mostIssuedCertThisWeek()).isEqualTo("BARANGAY CLEARANCE");
            assertThat(response.mostIssuedCertCountThisWeek()).isEqualTo(4L);
            assertThat(response.weekStartDate()).isNotNull();
            assertThat(response.weekEndDate()).isNotNull();
        }

        /**
         * Verifies fallback to all-time active certificates when current week has no top template.
         */
        @Test
        @DisplayName("shouldFallbackToAllActiveCertificates_whenWeekHasNoTopTemplate")
        void shouldFallbackToAllActiveCertificates_whenWeekHasNoTopTemplate() {
            // Given
            given(issuedCertificateRepository.countActiveCertificatesBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(0L)
                    .willReturn(0L);

            given(issuedCertificateRepository.countActiveCertificates()).willReturn(3L);
            given(issuedCertificateRepository.getMostIssuedTemplate(any(LocalDateTime.class), any(LocalDateTime.class), eq(null)))
                    .willReturn(List.of());

            IssuedCertificate c1 = new IssuedCertificate();
            CertificateTemplate t1 = new CertificateTemplate();
            t1.setCertTitle("BUILDING CLEARANCE");
            c1.setTemplate(t1);

            IssuedCertificate c2 = new IssuedCertificate();
            CertificateTemplate t2 = new CertificateTemplate();
            t2.setCertTitle("BUILDING CLEARANCE");
            c2.setTemplate(t2);

            given(issuedCertificateRepository.findAllActiveCertificates()).willReturn(List.of(c1, c2));

            // When
            ClearanceDashboardKpiResponse response = clearanceReportService.getDashboardKpis();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalIssuedToday()).isEqualTo(0L);
            assertThat(response.totalIssuedThisWeek()).isEqualTo(0L);
            assertThat(response.totalActiveClearances()).isEqualTo(3L);
            assertThat(response.mostIssuedCertThisWeek()).isEqualTo("BUILDING CLEARANCE");
            assertThat(response.mostIssuedCertCountThisWeek()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("getDailyTrendReport Method Tests")
    class GetDailyTrendReportTests {

        /**
         * Verifies calculation of 7-day daily trend report with zero-fill handling.
         */
        @Test
        @DisplayName("shouldComputeAccurate7DayDailyTrend_withZeroFill")
        void shouldComputeAccurate7DayDailyTrend_withZeroFill() {
            // Given
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(6);

            ClearanceDailyTrendProjection p1 = mock(ClearanceDailyTrendProjection.class);
            given(p1.getDate()).willReturn(end);
            given(p1.getCount()).willReturn(3L);

            given(issuedCertificateRepository.getDailyIssuanceCounts(any(LocalDateTime.class), any(LocalDateTime.class), eq(null)))
                    .willReturn(List.of(p1));

            // When
            List<ClearanceDailyTrendResponse> trend = clearanceReportService.getDailyTrendReport(start, end, null);

            // Then
            assertThat(trend).hasSize(7);
            assertThat(trend.get(6).totalIssued()).isEqualTo(3L);
            assertThat(trend.get(0).totalIssued()).isEqualTo(0L);
        }
    }
}
