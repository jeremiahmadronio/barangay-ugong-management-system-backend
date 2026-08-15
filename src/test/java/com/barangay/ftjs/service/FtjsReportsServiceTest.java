package com.barangay.ftjs.service;

import com.barangay.ftjs.dto.FtjsReportFilterDto;
import com.barangay.ftjs.dto.TrendPointDto;
import com.barangay.ftjs.dto.TrendReportResponse;
import com.barangay.ftjs.projection.TrendPointProjection;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FtjsReportsServiceTest {

    @Mock
    private FirstTimeJobSeekerRepository ftjsRepository;

    @InjectMocks
    private FtjsReportsService ftjsReportsService;

    private TrendPointProjection mockProjection(String label, Long count, LocalDate pointDate) {
        TrendPointProjection projection = mock(TrendPointProjection.class);
        when(projection.getLabel()).thenReturn(label);
        when(projection.getIssuedCount()).thenReturn(count);
        when(projection.getPointDate()).thenReturn(pointDate);
        return projection;
    }

    @Test
    @DisplayName("Should generate Daily trend report when date range is <= 31 days")
    void testDailyTrendReport() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 10);
        FtjsReportFilterDto filter = new FtjsReportFilterDto(start, end, "All Purposes", "All", "All Statuses", null);

        List<TrendPointProjection> mockData = List.of(
                mockProjection("Aug 01", 5L, LocalDate.of(2026, 8, 1)),
                mockProjection("Aug 02", 12L, LocalDate.of(2026, 8, 2)),
                mockProjection("Aug 03", 2L, LocalDate.of(2026, 8, 3))
        );

        when(ftjsRepository.getTimelineTrendData(
                eq(start), eq(end), eq("1 day"), eq("day"), eq("Mon DD"),
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(mockData);

        TrendReportResponse response = ftjsReportsService.generateAdaptiveTrendReport(filter);

        assertNotNull(response);
        assertEquals("Daily", response.granularityLabel());
        assertEquals("Last 10 days", response.rangeLabel());
        assertEquals(3, response.points().size());

        assertNotNull(response.busiest());
        assertEquals("Aug 02", response.busiest().label());
        assertEquals(12L, response.busiest().issued());

        assertNotNull(response.slowest());
        assertEquals("Aug 03", response.slowest().label());
        assertEquals(2L, response.slowest().issued());
    }

    @Test
    @DisplayName("Should generate Monthly trend report when date range > 31 days")
    void testMonthlyTrendReport() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 6, 30);
        FtjsReportFilterDto filter = new FtjsReportFilterDto(start, end, "Local Employment", "Male", "ISSUED", "College Graduate");

        List<TrendPointProjection> mockData = List.of(
                mockProjection("Jan 2026", 45L, LocalDate.of(2026, 1, 1)),
                mockProjection("Feb 2026", 80L, LocalDate.of(2026, 2, 1))
        );

        when(ftjsRepository.getTimelineTrendData(
                eq(start), eq(end), eq("1 month"), eq("month"), eq("Mon YYYY"),
                eq("Local Employment"), eq("Male"), eq("ISSUED"), eq("College Graduate")
        )).thenReturn(mockData);

        TrendReportResponse response = ftjsReportsService.generateAdaptiveTrendReport(filter);

        assertNotNull(response);
        assertEquals("Monthly", response.granularityLabel());
        assertEquals("Jan 2026 – Jun 2026", response.rangeLabel());
        assertEquals(2, response.points().size());
        assertEquals("Feb 2026", response.busiest().label());
        assertEquals(80L, response.busiest().issued());
    }

    @Test
    @DisplayName("Should swap start and end dates if start is after end (Date Inversion Guard)")
    void testDateInversionGuard() {
        LocalDate start = LocalDate.of(2026, 8, 10);
        LocalDate end = LocalDate.of(2026, 8, 1);
        FtjsReportFilterDto filter = new FtjsReportFilterDto(start, end, null, null, null, null);

        when(ftjsRepository.getTimelineTrendData(
                eq(end), eq(start), anyString(), anyString(), anyString(),
                isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of());

        TrendReportResponse response = ftjsReportsService.generateAdaptiveTrendReport(filter);

        assertNotNull(response);
        assertEquals("Last 10 days", response.rangeLabel());
        verify(ftjsRepository).getTimelineTrendData(
                eq(end), eq(start), eq("1 day"), eq("day"), eq("Mon DD"),
                isNull(), isNull(), isNull(), isNull()
        );
    }

    @Test
    @DisplayName("Should generate Education Distribution report matching global filters")
    void testEducationDistribution() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 10);
        FtjsReportFilterDto filter = new FtjsReportFilterDto(start, end, "Employment Application", "Female", "ISSUED", null);

        com.barangay.ftjs.projection.EducationDistributionProjection p1 = mock(com.barangay.ftjs.projection.EducationDistributionProjection.class);
        when(p1.getEducationalAttainment()).thenReturn("College Graduate");
        when(p1.getApplicantCount()).thenReturn(25L);

        com.barangay.ftjs.projection.EducationDistributionProjection p2 = mock(com.barangay.ftjs.projection.EducationDistributionProjection.class);
        when(p2.getEducationalAttainment()).thenReturn("High School Graduate");
        when(p2.getApplicantCount()).thenReturn(10L);

        when(ftjsRepository.getEducationDistribution(
                eq(start), eq(end), eq("Employment Application"), eq("Female"), eq("ISSUED"), isNull()
        )).thenReturn(List.of(p1, p2));

        com.barangay.ftjs.dto.EducationDistributionResponse response = ftjsReportsService.generateEducationDistribution(filter);

        assertNotNull(response);
        assertEquals(2, response.points().size());
        assertEquals("College Graduate", response.highestLabel());
        assertEquals(25L, response.highestCount());
        assertEquals(35L, response.totalVolume());
    }

    @Test
    @DisplayName("Should normalize 'Other (Please specify)' to 'Other' for custom purpose matching")
    void testOtherPurposeNormalization() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 10);
        FtjsReportFilterDto filter = new FtjsReportFilterDto(start, end, "Other (Please specify)", "Male", "ISSUED", null);

        when(ftjsRepository.getEducationDistribution(
                eq(start), eq(end), eq("Other"), eq("Male"), eq("ISSUED"), isNull()
        )).thenReturn(List.of());

        ftjsReportsService.generateEducationDistribution(filter);

        verify(ftjsRepository).getEducationDistribution(
                eq(start), eq(end), eq("Other"), eq("Male"), eq("ISSUED"), isNull()
        );
    }

    @Test
    @DisplayName("Should generate Purpose Distribution report with correct counts and shares")
    void testPurposeDistribution() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 10);
        FtjsReportFilterDto filter = new FtjsReportFilterDto(start, end, null, null, null, null);

        com.barangay.ftjs.projection.PurposeDistributionProjection p1 = mock(com.barangay.ftjs.projection.PurposeDistributionProjection.class);
        when(p1.getPurpose()).thenReturn("Local Employment");
        when(p1.getApplicantCount()).thenReturn(2L);

        com.barangay.ftjs.projection.PurposeDistributionProjection p2 = mock(com.barangay.ftjs.projection.PurposeDistributionProjection.class);
        when(p2.getPurpose()).thenReturn("Other");
        when(p2.getApplicantCount()).thenReturn(3L);

        when(ftjsRepository.getPurposeDistribution(
                eq(start), eq(end), isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of(p1, p2));

        com.barangay.ftjs.dto.PurposeDistributionResponse response = ftjsReportsService.generatePurposeDistribution(filter);

        assertNotNull(response);
        assertEquals(5L, response.totalVolume());
        assertEquals(2, response.points().size());
        assertEquals("Local Employment", response.points().get(0).purpose());
        assertEquals(40.0, response.points().get(0).share());
        assertEquals("Other", response.points().get(1).purpose());
        assertEquals(60.0, response.points().get(1).share());
    }

    @Test
    @DisplayName("Should generate Issuance Type Distribution report (Original vs Re-issuance) with correct counts and shares")
    void testIssuanceTypeDistribution() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 10);
        FtjsReportFilterDto filter = new FtjsReportFilterDto(start, end, null, null, null, null);

        com.barangay.ftjs.projection.IssuanceTypeProjection i1 = mock(com.barangay.ftjs.projection.IssuanceTypeProjection.class);
        when(i1.getIssuanceType()).thenReturn("Original issuance");
        when(i1.getApplicantCount()).thenReturn(4L);

        com.barangay.ftjs.projection.IssuanceTypeProjection i2 = mock(com.barangay.ftjs.projection.IssuanceTypeProjection.class);
        when(i2.getIssuanceType()).thenReturn("Re-issuance");
        when(i2.getApplicantCount()).thenReturn(1L);

        when(ftjsRepository.getIssuanceTypeDistribution(
                eq(start), eq(end), isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of(i1, i2));

        com.barangay.ftjs.dto.IssuanceTypeDistributionResponse response = ftjsReportsService.generateIssuanceTypeDistribution(filter);

        assertNotNull(response);
        assertEquals(5L, response.totalVolume());
        assertEquals(2, response.points().size());
        assertEquals("Original issuance", response.points().get(0).typeLabel());
        assertEquals(80.0, response.points().get(0).share());
        assertEquals("Re-issuance", response.points().get(1).typeLabel());
        assertEquals(20.0, response.points().get(1).share());
    }

    @Test
    @DisplayName("Should generate paginated Report Table matching global filters")
    void testReportTable() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 10);
        FtjsReportFilterDto filter = new FtjsReportFilterDto(start, end, "Other (Please specify)", "Male", "ISSUED", null);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        com.barangay.ftjs.projection.FtjsReportTableProjection p1 = mock(com.barangay.ftjs.projection.FtjsReportTableProjection.class);
        when(p1.getId()).thenReturn(1L);
        when(p1.getApplicationNo()).thenReturn("2026-FTJS-5516");
        when(p1.getApplicantName()).thenReturn("Adelaida Ocampo Perez");
        when(p1.getIssuanceCount()).thenReturn(1L);
        when(p1.getPurpose()).thenReturn("Employment Application");
        when(p1.getStatus()).thenReturn("ISSUED");

        org.springframework.data.domain.Page<com.barangay.ftjs.projection.FtjsReportTableProjection> mockPage = new org.springframework.data.domain.PageImpl<>(List.of(p1));

        when(ftjsRepository.findReportTableFiltered(
                eq(start),
                eq(end),
                eq("ISSUED"),
                eq("Other"),
                eq("Male"),
                isNull(),
                eq("search"),
                eq(pageable)
        )).thenReturn(mockPage);

        org.springframework.data.domain.Page<com.barangay.ftjs.dto.FtjsReportTableResponse> result = ftjsReportsService.generateReportTable(filter, "search", pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("2026-FTJS-5516", result.getContent().get(0).applicationNo());
    }

    @Test
    @DisplayName("Should generate Report KPI cards matching global filters")
    void testReportKpi() {
        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 10);
        FtjsReportFilterDto filter = new FtjsReportFilterDto(start, end, null, null, null, null);

        com.barangay.ftjs.projection.IssuanceTypeProjection i1 = mock(com.barangay.ftjs.projection.IssuanceTypeProjection.class);
        when(i1.getIssuanceType()).thenReturn("Original issuance");
        when(i1.getApplicantCount()).thenReturn(29L);

        com.barangay.ftjs.projection.IssuanceTypeProjection i2 = mock(com.barangay.ftjs.projection.IssuanceTypeProjection.class);
        when(i2.getIssuanceType()).thenReturn("Re-issuance");
        when(i2.getApplicantCount()).thenReturn(4L);

        when(ftjsRepository.getIssuanceTypeDistribution(
                eq(start), eq(end), isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of(i1, i2));

        com.barangay.ftjs.projection.PurposeDistributionProjection p1 = mock(com.barangay.ftjs.projection.PurposeDistributionProjection.class);
        when(p1.getPurpose()).thenReturn("Employment Application");
        when(p1.getApplicantCount()).thenReturn(9L);

        when(ftjsRepository.getPurposeDistribution(
                eq(start), eq(end), isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of(p1));

        com.barangay.ftjs.projection.AgeRangeDistributionProjection a1 = mock(com.barangay.ftjs.projection.AgeRangeDistributionProjection.class);
        when(a1.getAgeRange()).thenReturn("Over 40");
        when(a1.getApplicantCount()).thenReturn(33L);

        when(ftjsRepository.getAgeRangeDistribution(
                eq(start), eq(end), isNull(), isNull(), isNull(), isNull()
        )).thenReturn(List.of(a1));

        com.barangay.ftjs.dto.FtjsReportKpiResponse kpi = ftjsReportsService.generateReportKpi(filter);

        assertNotNull(kpi);
        assertEquals(33L, kpi.totalRecords());
        assertEquals(29L, kpi.firstTimeIssuances());
        assertEquals(87.9, kpi.firstTimeIssuanceShare());
        assertEquals("Employment Application", kpi.mostCommonPurpose());
        assertEquals("Over 40", kpi.mostCommonAgeRange());
    }
}
