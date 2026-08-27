package com.barangay.cases.blotter.service;

import com.barangay.cases.blotter.dto.DailyCaseTrendResponse;
import com.barangay.cases.blotter.dto.MonthlyCaseTrendResponse;
import com.barangay.cases.shared.projection.CaseCountByDayProjection;
import com.barangay.cases.shared.projection.CaseCountByMonthProjection;
import com.barangay.cases.shared.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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




}
