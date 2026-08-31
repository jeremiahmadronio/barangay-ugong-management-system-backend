package com.barangay.cases.lupon.service;

import com.barangay.cases.blotter.dto.DailyCaseTrendResponse;
import com.barangay.cases.blotter.dto.MonthlyCaseTrendResponse;
import com.barangay.cases.lupon.dto.LuponReportKpiResponse;
import com.barangay.cases.shared.projection.CaseCountByDayProjection;
import com.barangay.cases.shared.projection.CaseCountByMonthProjection;
import com.barangay.cases.shared.projection.LuponReportKpiProjection;
import com.barangay.cases.shared.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service handling analytics, KPI metrics, and conciliation trends exclusively for Lupong Tagapamayapa.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LuponReports {

    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM dd");

    private final CaseRepository caseRepository;

    /**
     * Calculates Lupon KPI statistics (Total Referred, Settled, Settlement Rate, Active Conciliations, CFA Issued)
     * within a specified date range.
     *
     * @param startDate the inclusive starting date
     * @param endDate   the inclusive ending date
     * @return {@link LuponReportKpiResponse}
     */
    @Transactional(readOnly = true)
    public LuponReportKpiResponse getReportKpi(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        LuponReportKpiProjection metrics = caseRepository.getLuponReportKpiMetrics(startDateTime, endDateTime);

        long totalReferred = (metrics != null && metrics.getTotalReferred() != null)
                ? metrics.getTotalReferred().longValue()
                : 0L;

        long settledCases = (metrics != null && metrics.getSettledCases() != null)
                ? metrics.getSettledCases().longValue()
                : 0L;

        long activeConciliations = (metrics != null && metrics.getActiveConciliations() != null)
                ? metrics.getActiveConciliations().longValue()
                : 0L;

        long cfaIssued = (metrics != null && metrics.getCfaIssued() != null)
                ? metrics.getCfaIssued().longValue()
                : 0L;

        double settlementRate = 0.0;
        if (totalReferred > 0) {
            double rate = ((double) settledCases / totalReferred) * 100.0;
            settlementRate = Math.round(rate * 100.0) / 100.0;
        }

        return new LuponReportKpiResponse(
                startDate,
                endDate,
                totalReferred,
                settledCases,
                settlementRate,
                activeConciliations,
                cfaIssued
        );
    }

    /**
     * Generates a monthly conciliation case trend report for Lupon cases within a date range.
     *
     * @param startDate inclusive starting date
     * @param endDate   inclusive ending date
     * @return list of {@link MonthlyCaseTrendResponse} chronologically ordered
     */
    @Transactional(readOnly = true)
    public List<MonthlyCaseTrendResponse> getMonthlyTrendReport(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<CaseCountByMonthProjection> dbResult = caseRepository.getMonthlyLuponCaseCount(startDateTime, endDateTime);

        Map<YearMonth, Long> countMap = (dbResult != null)
                ? dbResult.stream().collect(
                Collectors.toMap(
                        p -> YearMonth.of(p.getYear(), p.getMonth()),
                        CaseCountByMonthProjection::getCount,
                        (existing, replacement) -> existing
                ))
                : Collections.emptyMap();

        List<MonthlyCaseTrendResponse> responseList = new ArrayList<>();
        YearMonth current = YearMonth.from(startDate);
        YearMonth targetEnd = YearMonth.from(endDate);

        while (!current.isAfter(targetEnd)) {
            long totalCases = countMap.getOrDefault(current, 0L);
            String monthLabel = current.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " " + current.getYear();

            responseList.add(new MonthlyCaseTrendResponse(
                    monthLabel,
                    current.getYear(),
                    current.getMonthValue(),
                    totalCases
            ));

            current = current.plusMonths(1);
        }

        return responseList;
    }

    /**
     * Generates a daily conciliation case trend report for Lupon cases within a date range.
     *
     * @param startDate inclusive starting date
     * @param endDate   inclusive ending date
     * @return list of {@link DailyCaseTrendResponse} chronologically ordered
     */
    @Transactional(readOnly = true)
    public List<DailyCaseTrendResponse> getDailyTrendReport(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<CaseCountByDayProjection> dbResults = caseRepository.getDailyLuponCaseCounts(startDateTime, endDateTime);

        Map<LocalDate, Long> countMap = new HashMap<>();
        if (dbResults != null) {
            for (CaseCountByDayProjection p : dbResults) {
                if (p.getDate() != null) {
                    countMap.put(p.getDate(), p.getCount() != null ? p.getCount() : 0L);
                }
            }
        }

        List<DailyCaseTrendResponse> responseList = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            long totalCases = countMap.getOrDefault(current, 0L);

            responseList.add(new DailyCaseTrendResponse(
                    current,
                    current.format(DAY_LABEL_FORMATTER),
                    current.getDayOfMonth(),
                    totalCases
            ));

            current = current.plusDays(1);
        }

        return responseList;
    }

    /**
     * Generates a complete status distribution breakdown exclusively for Lupon-referred cases.
     *
     * @param startDate inclusive start date
     * @param endDate   inclusive end date
     * @return {@link com.barangay.cases.blotter.dto.CaseStatusBreakdownResponse}
     */
    @Transactional(readOnly = true)
    public com.barangay.cases.blotter.dto.CaseStatusBreakdownResponse getStatusBreakdownReport(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<com.barangay.cases.shared.projection.CaseStatusCountProjection> dbResults = caseRepository.getLuponCaseStatusCounts(
                startDateTime,
                endDateTime
        );

        Map<com.barangay.zshared.enumerated.CaseStatus, Long> countMap = new HashMap<>();
        if (dbResults != null) {
            for (com.barangay.cases.shared.projection.CaseStatusCountProjection p : dbResults) {
                if (p != null && p.getStatus() != null) {
                    Object statusObj = p.getStatus();
                    com.barangay.zshared.enumerated.CaseStatus status = null;
                    if (statusObj instanceof com.barangay.zshared.enumerated.CaseStatus cs) {
                        status = cs;
                    } else if (statusObj instanceof String str) {
                        try {
                            status = com.barangay.zshared.enumerated.CaseStatus.valueOf(str.trim().toUpperCase());
                        } catch (IllegalArgumentException ignored) {}
                    }
                    if (status != null) {
                        countMap.put(status, p.getCount() != null ? p.getCount() : 0L);
                    }
                }
            }
        }

        long totalCases = countMap.values().stream().mapToLong(Long::longValue).sum();

        List<com.barangay.cases.blotter.dto.CaseStatusItemDto> breakdownList = new ArrayList<>();

        for (com.barangay.zshared.enumerated.CaseStatus status : com.barangay.zshared.enumerated.CaseStatus.values()) {
            long count = countMap.getOrDefault(status, 0L);
            if (count > 0) {
                double rawPercentage = totalCases > 0 ? ((double) count / totalCases) * 100.0 : 0.0;
                double percentage = java.math.BigDecimal.valueOf(rawPercentage)
                        .setScale(2, java.math.RoundingMode.HALF_UP)
                        .doubleValue();

                String humanReadableLabel = formatEnumLabel(status.name());

                breakdownList.add(new com.barangay.cases.blotter.dto.CaseStatusItemDto(
                        status,
                        humanReadableLabel,
                        count,
                        percentage
                ));
            }
        }

        breakdownList.sort((a, b) -> Long.compare(b.count(), a.count()));

        return new com.barangay.cases.blotter.dto.CaseStatusBreakdownResponse(
                startDate,
                endDate,
                null,
                totalCases,
                breakdownList
        );
    }

    private String formatEnumLabel(String enumName) {
        if (enumName == null || enumName.isEmpty()) return "";
        String[] words = enumName.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
