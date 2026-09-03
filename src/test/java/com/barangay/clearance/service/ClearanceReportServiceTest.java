package com.barangay.clearance.service;

import com.barangay.clearance.dto.ClearanceDailyTrendResponse;
import com.barangay.clearance.dto.ClearanceDashboardKpiResponse;
import com.barangay.clearance.model.CertificateTemplate;
import com.barangay.clearance.model.IssuedCertificate;
import com.barangay.clearance.projection.ClearanceDailyTrendProjection;
import com.barangay.clearance.projection.MostIssuedCertProjection;
import com.barangay.clearance.repository.IssuedCertificateRepository;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClearanceReportServiceTest {

    @Mock
    private IssuedCertificateRepository issuedCertificateRepository;

    @InjectMocks
    private ClearanceReportService clearanceReportService;

    @Test
    @DisplayName("should compute accurate Clearance Dashboard Weekly KPIs")
    void shouldComputeAccurateDashboardKpis() {
        when(issuedCertificateRepository.countActiveCertificatesBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(2L) // Today count
                .thenReturn(5L); // Week count

        when(issuedCertificateRepository.countActiveCertificates()).thenReturn(5L);

        MostIssuedCertProjection proj = mock(MostIssuedCertProjection.class);
        when(proj.getCertTitle()).thenReturn("BARANGAY CLEARANCE");
        when(proj.getCount()).thenReturn(4L);
        when(issuedCertificateRepository.getMostIssuedTemplate(any(LocalDateTime.class), any(LocalDateTime.class), eq(null)))
                .thenReturn(List.of(proj));

        ClearanceDashboardKpiResponse response = clearanceReportService.getDashboardKpis();

        assertThat(response).isNotNull();
        assertThat(response.totalIssuedToday()).isEqualTo(2L);
        assertThat(response.totalIssuedThisWeek()).isEqualTo(5L);
        assertThat(response.totalActiveClearances()).isEqualTo(5L);
        assertThat(response.mostIssuedCertThisWeek()).isEqualTo("BARANGAY CLEARANCE");
        assertThat(response.mostIssuedCertCountThisWeek()).isEqualTo(4L);
        assertThat(response.weekStartDate()).isNotNull();
        assertThat(response.weekEndDate()).isNotNull();
    }

    @Test
    @DisplayName("should fallback to all active certificates when week is zero")
    void shouldFallbackWhenWeekHasNoTopTemplate() {
        when(issuedCertificateRepository.countActiveCertificatesBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L)
                .thenReturn(0L);

        when(issuedCertificateRepository.countActiveCertificates()).thenReturn(3L);
        when(issuedCertificateRepository.getMostIssuedTemplate(any(LocalDateTime.class), any(LocalDateTime.class), eq(null)))
                .thenReturn(List.of());

        IssuedCertificate c1 = new IssuedCertificate();
        CertificateTemplate t1 = new CertificateTemplate();
        t1.setCertTitle("BUILDING CLEARANCE");
        c1.setTemplate(t1);

        IssuedCertificate c2 = new IssuedCertificate();
        CertificateTemplate t2 = new CertificateTemplate();
        t2.setCertTitle("BUILDING CLEARANCE");
        c2.setTemplate(t2);

        when(issuedCertificateRepository.findAllActiveCertificates()).thenReturn(List.of(c1, c2));

        ClearanceDashboardKpiResponse response = clearanceReportService.getDashboardKpis();

        assertThat(response).isNotNull();
        assertThat(response.totalIssuedToday()).isEqualTo(0L);
        assertThat(response.totalIssuedThisWeek()).isEqualTo(0L);
        assertThat(response.totalActiveClearances()).isEqualTo(3L);
        assertThat(response.mostIssuedCertThisWeek()).isEqualTo("BUILDING CLEARANCE");
        assertThat(response.mostIssuedCertCountThisWeek()).isEqualTo(2L);
    }

    @Test
    @DisplayName("should compute accurate 7-day daily issuance trend with zero-fill")
    void shouldComputeAccurate7DayDailyTrend() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(6);

        ClearanceDailyTrendProjection p1 = mock(ClearanceDailyTrendProjection.class);
        when(p1.getDate()).thenReturn(end);
        when(p1.getCount()).thenReturn(3L);

        when(issuedCertificateRepository.getDailyIssuanceCounts(any(LocalDateTime.class), any(LocalDateTime.class), eq(null)))
                .thenReturn(List.of(p1));

        List<ClearanceDailyTrendResponse> trend = clearanceReportService.getDailyTrendReport(start, end, null);

        assertThat(trend).hasSize(7);
        assertThat(trend.get(6).totalIssued()).isEqualTo(3L);
        assertThat(trend.get(0).totalIssued()).isEqualTo(0L);
    }
}
