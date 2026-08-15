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
}
