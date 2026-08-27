package com.barangay.cases.blotter.service;

import com.barangay.cases.blotter.dto.DailyCaseTrendResponse;
import com.barangay.cases.blotter.dto.MonthlyCaseTrendResponse;
import com.barangay.cases.blotter.dto.SettlementEfficiencyResponse;
import com.barangay.cases.shared.projection.CaseCountByDayProjection;
import com.barangay.cases.shared.projection.CaseCountByMonthProjection;
import com.barangay.cases.shared.projection.SettlementMetricsProjection;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlotterReportService {

    private final CaseRepository caseRepository;
    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM d");


    @Transactional
    public List<MonthlyCaseTrendResponse> getMonthlyTrendReport(
            LocalDate startDate,
            LocalDate endDate,
            Long departmentId
    ){
        if(startDate.isAfter(endDate)){
            throw new IllegalArgumentException("Start date is after end date");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<CaseCountByMonthProjection> dbResult = caseRepository.getMonthlyCaseCount(startDateTime, endDateTime, departmentId);

        Map<YearMonth, Long> countMap = dbResult.stream().collect(
                Collectors.toMap(
                        p -> YearMonth.of(p.getYear(), p.getMonth()),
                        CaseCountByMonthProjection::getCount
                )
        );

        List<MonthlyCaseTrendResponse> responseList = new ArrayList<>();
        YearMonth current = YearMonth.from(startDate);
        YearMonth targetEnd = YearMonth.from(endDate);

        while (!current.isAfter(targetEnd)) {
            long totalCases = countMap.getOrDefault(current, 0L);
            String monthLabel = current.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase()
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


    @Transactional(readOnly = true)
    public List<DailyCaseTrendResponse> getDailyTrendReport(
            LocalDate startDate,
            LocalDate endDate,
            Long departmentId
    ) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<CaseCountByDayProjection> dbResults = caseRepository.getDailyCaseCounts(
                startDateTime,
                endDateTime,
                departmentId
        );

        Map<LocalDate, Long> countMap = dbResults.stream().collect(
                Collectors.toMap(
                        CaseCountByDayProjection::getDate,
                        CaseCountByDayProjection::getCount
                )
        );

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
     * Generates a settlement efficiency report comparing formal complaints against settled cases.
     *
     * @param startDate    the start date filter
     * @param endDate      the end date filter
     * @param departmentId the department filter (optional)
     * @return {@link SettlementEfficiencyResponse} with aggregated counts and calculated percentage
     */
    @Transactional(readOnly = true)
    public SettlementEfficiencyResponse getSettlementEfficiency(
            LocalDate startDate,
            LocalDate endDate,
            Long departmentId
    ) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        SettlementMetricsProjection metrics = caseRepository.getSettlementEfficiencyMetrics(
                startDateTime,
                endDateTime,
                departmentId,
                FilingType.FORMAL_COMPLAINT,
                CaseStatus.SETTLED
        );

        long totalCases = (metrics != null && metrics.getTotalCases() != null) ? metrics.getTotalCases() : 0L;
        long totalFormal = (metrics != null && metrics.getTotalFormalComplaints() != null) ? metrics.getTotalFormalComplaints() : 0L;
        long totalSettled = (metrics != null && metrics.getTotalSettledCases() != null) ? metrics.getTotalSettledCases() : 0L;


        double settlementRate = 0.0;
        if (totalFormal > 0) {
            double rawRate = ((double) totalSettled / totalFormal) * 100.0;
            settlementRate = BigDecimal.valueOf(rawRate)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new SettlementEfficiencyResponse(
                startDate,
                endDate,
                departmentId,
                totalCases,
                totalFormal,
                totalSettled,
                settlementRate
        );
    }




}
