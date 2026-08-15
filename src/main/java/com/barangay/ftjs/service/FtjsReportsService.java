package com.barangay.ftjs.service;

import com.barangay.ftjs.dto.FtjsReportFilterDto;
import com.barangay.ftjs.dto.TrendPointDto;
import com.barangay.ftjs.dto.TrendReportResponse;
import com.barangay.ftjs.projection.TrendPointProjection;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FtjsReportsService {

    private final FirstTimeJobSeekerRepository ftjsRepository;

    public TrendReportResponse generateAdaptiveTrendReport(FtjsReportFilterDto filter) {
        LocalDate rawEnd = filter.dateTo() != null ? filter.dateTo() : LocalDate.now();
        LocalDate rawStart = filter.dateFrom() != null ? filter.dateFrom() : rawEnd.minusDays(14);

        // Date Inversion Guard
        LocalDate start = rawStart.isAfter(rawEnd) ? rawEnd : rawStart;
        LocalDate end = rawStart.isAfter(rawEnd) ? rawStart : rawEnd;

        // Dynamic Resolution: <= 31 days = Daily, > 31 days = Monthly
        long totalDays = ChronoUnit.DAYS.between(start, end) + 1;
        boolean isDaily = totalDays <= 31;

        String intervalStep = isDaily ? "1 day" : "1 month";
        String truncType    = isDaily ? "day"   : "month";
        String dateFormat   = isDaily ? "Mon DD" : "Mon YYYY";
        String granularity  = isDaily ? "Daily" : "Monthly";

        String rangeLabel = isDaily
                ? String.format("Last %d days", totalDays)
                : String.format("%s – %s",
                start.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                end.format(DateTimeFormatter.ofPattern("MMM yyyy")));

        List<TrendPointProjection> queryResults = ftjsRepository.getTimelineTrendData(
                start, end, intervalStep, truncType, dateFormat,
                normalize(filter.purpose()),
                normalize(filter.sex()),
                normalize(filter.status()),
                normalize(filter.educationalAttainment())
        );

        List<TrendPointDto> points = new ArrayList<>();
        TrendPointDto busiest = null;
        TrendPointDto slowest = null;

        for (TrendPointProjection record : queryResults) {
            long count = record.getIssuedCount() != null ? record.getIssuedCount() : 0L;
            TrendPointDto point = new TrendPointDto(record.getLabel(), count, record.getPointDate());
            points.add(point);

            if (busiest == null || count > busiest.issued()) busiest = point;
            if (slowest == null || count < slowest.issued()) slowest = point;
        }

        return new TrendReportResponse(granularity, rangeLabel, points, busiest, slowest);
    }

    private String normalize(String input) {
        if (input == null || input.trim().isEmpty() || "All".equalsIgnoreCase(input.trim())
                || "All Purposes".equalsIgnoreCase(input.trim()) || "All Statuses".equalsIgnoreCase(input.trim())) {
            return null;
        }
        return input.trim();
    }
}
