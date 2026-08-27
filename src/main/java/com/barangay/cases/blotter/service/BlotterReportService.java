package com.barangay.cases.blotter.service;

import com.barangay.cases.blotter.dto.*;
import com.barangay.cases.shared.projection.*;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.repository.CaseSessionRepository;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import com.barangay.zshared.enumerated.SessionStatus;
import com.barangay.zshared.enumerated.SessionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlotterReportService {

    private final CaseRepository caseRepository;
    private final CaseSessionRepository caseSessionRepository;
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

        long totalCases = (metrics != null && metrics.getTotalCases() != null) ? metrics.getTotalCases().longValue() : 0L;
        long totalFormal = (metrics != null && metrics.getTotalFormalComplaints() != null) ? metrics.getTotalFormalComplaints().longValue() : 0L;
        long totalSettled = (metrics != null && metrics.getTotalSettledCases() != null) ? metrics.getTotalSettledCases().longValue() : 0L;


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



    /**
     * Generates a complete status distribution breakdown for cases within the specified criteria.
     *
     * @param startDate    inclusive start date
     * @param endDate      inclusive end date
     * @param departmentId optional department ID filter
     * @return {@link CaseStatusBreakdownResponse} containing total cases and per-status metrics
     */
    @Transactional(readOnly = true)
    public CaseStatusBreakdownResponse getStatusBreakdownReport(
            LocalDate startDate,
            LocalDate endDate,
            Long departmentId
    ) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<CaseStatusCountProjection> dbResults = caseRepository.getCaseStatusCounts(
                startDateTime,
                endDateTime,
                departmentId
        );

        Map<CaseStatus, Long> countMap = new HashMap<>();
        if (dbResults != null) {
            for (CaseStatusCountProjection p : dbResults) {
                if (p != null && p.getStatus() != null) {
                    Object statusObj = p.getStatus();
                    CaseStatus status = null;
                    if (statusObj instanceof CaseStatus cs) {
                        status = cs;
                    } else if (statusObj instanceof String str) {
                        try {
                            status = CaseStatus.valueOf(str.trim().toUpperCase());
                        } catch (IllegalArgumentException ignored) {}
                    }
                    if (status != null) {
                        countMap.put(status, p.getCount() != null ? p.getCount() : 0L);
                    }
                }
            }
        }

        long totalCases = countMap.values().stream().mapToLong(Long::longValue).sum();

        List<CaseStatusItemDto> breakdownList = new ArrayList<>();

        for (CaseStatus status : CaseStatus.values()) {
            long count = countMap.getOrDefault(status, 0L);
            if (count > 0) {
                double rawPercentage = ((double) count / totalCases) * 100.0;
                double percentage = BigDecimal.valueOf(rawPercentage)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

                String humanReadableLabel = formatEnumLabel(status.name());

                breakdownList.add(new CaseStatusItemDto(
                        status,
                        humanReadableLabel,
                        count,
                        percentage
                ));
            }
        }

        breakdownList.sort(Comparator.comparingLong(CaseStatusItemDto::count).reversed());

        return new CaseStatusBreakdownResponse(
                startDate,
                endDate,
                departmentId,
                totalCases,
                breakdownList
        );
    }

    private String formatEnumLabel(String enumName) {
        String[] words = enumName.toLowerCase().split("_");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return formatted.toString().trim();
    }


    /**
     * Computes the resolution breakdown and percentage rates for formal complaints.
     *
     * @param startDate    start date filter
     * @param endDate      end date filter
     * @param departmentId optional department ID filter (e.g., Blotter Department ID)
     * @return {@link FormalComplaintReportResponse} containing calculated metrics
     */
    @Transactional(readOnly = true)
    public FormalComplaintReportResponse getResolutionReport(
            LocalDate startDate,
            LocalDate endDate,
            Long departmentId
    ) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        FormalComplaintResolutionProjection metrics = caseRepository.getFormalComplaintResolutionMetrics(
                startDateTime,
                endDateTime,
                departmentId,
                FilingType.FORMAL_COMPLAINT,
                CaseStatus.SETTLED
        );

        long totalFormal = (metrics != null && metrics.getTotalFormalComplaints() != null)
                ? metrics.getTotalFormalComplaints().longValue() : 0L;
        long settledAtBlotter = (metrics != null && metrics.getSettledAtBlotter() != null)
                ? metrics.getSettledAtBlotter().longValue() : 0L;
        long referredToLupon = (metrics != null && metrics.getReferredToLupon() != null)
                ? metrics.getReferredToLupon().longValue() : 0L;
        long pendingAtBlotter = (metrics != null && metrics.getPendingAtBlotter() != null)
                ? metrics.getPendingAtBlotter().longValue() : 0L;

        // Compute percentage rates rounded to 2 decimal places
        double settledRate = calculatePercentage(settledAtBlotter, totalFormal);
        double referredRate = calculatePercentage(referredToLupon, totalFormal);
        double pendingRate = calculatePercentage(pendingAtBlotter, totalFormal);

        ResolutionBreakdownDto breakdown = new ResolutionBreakdownDto(
                settledAtBlotter,
                settledRate,
                referredToLupon,
                referredRate,
                pendingAtBlotter,
                pendingRate
        );

        return new FormalComplaintReportResponse(
                startDate,
                endDate,
                departmentId,
                totalFormal,
                breakdown,
                settledRate
        );
    }

    private double calculatePercentage(long count, long total) {
        if (total == 0 || count == 0) {
            return 0.0;
        }
        double raw = ((double) count / total) * 100.0;
        return BigDecimal.valueOf(raw)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Calculates date-filtered KPI metrics for Blotter department reports.
     *
     * @param startDate    start date filter
     * @param endDate      end date filter
     * @param departmentId optional target department ID filter
     * @return {@link BlotterReportKpiResponse} with calculated counts and settlement rate
     */
    @Transactional(readOnly = true)
    public BlotterReportKpiResponse getReportKpi(
            LocalDate startDate,
            LocalDate endDate,
            Long departmentId
    ) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<CaseStatus> activeStatuses = List.of(
                CaseStatus.UNDER_MEDIATION,
                CaseStatus.UNDER_CONCILIATION
        );

        BlotterReportKpiProjection metrics = caseRepository.getBlotterReportKpiMetrics(
                startDateTime,
                endDateTime,
                departmentId,
                CaseStatus.SETTLED,
                activeStatuses
        );

        long totalCases = (metrics != null && metrics.getTotalCases() != null) ? metrics.getTotalCases().longValue() : 0L;
        long settledCases = (metrics != null && metrics.getSettledCases() != null) ? metrics.getSettledCases().longValue() : 0L;
        long activeMediations = (metrics != null && metrics.getActiveMediations() != null) ? metrics.getActiveMediations().longValue() : 0L;
        long luponReferrals = (metrics != null && metrics.getLuponReferrals() != null) ? metrics.getLuponReferrals().longValue() : 0L;

        double settlementRate = calculatePercentage(settledCases, totalCases);

        return new BlotterReportKpiResponse(
                startDate,
                endDate,
                departmentId,
                totalCases,
                settledCases,
                settlementRate,
                activeMediations,
                luponReferrals
        );
    }




    /**
     * Unresolved statuses at Blotter desk level.
     * Strictly EXCLUDES UNDER_CONCILIATION (Lupon jurisdiction) and final statuses (Settled, Dismissed, CFA).
     */
    private static final List<CaseStatus> UNRESOLVED_STATUSES = List.of(
            CaseStatus.PENDING,
            CaseStatus.UNDER_MEDIATION
    );

    /**
     * Generates real-time Dashboard KPIs for Blotter.
     *
     * @return {@link BlotterDashboardKpiResponse}
     */
    @Transactional(readOnly = true)
    public BlotterDashboardKpiResponse getDashboardKpis() {
        LocalDate today = LocalDate.now();

        LocalDate mondayDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sundayDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        LocalDateTime startOfWeek = mondayDate.atStartOfDay();
        LocalDateTime endOfWeek = sundayDate.atTime(LocalTime.MAX);

        BlotterDashboardKpiProjection caseMetrics = caseRepository.getBlotterDashboardCaseKpis(
                startOfWeek,
                endOfWeek,
                CaseStatus.PENDING,
                UNRESOLVED_STATUSES
        );

        long newCases = (caseMetrics != null && caseMetrics.getNewCasesThisWeek() != null)
                ? caseMetrics.getNewCasesThisWeek().longValue() : 0L;
        long pendingCases = (caseMetrics != null && caseMetrics.getPendingCases() != null)
                ? caseMetrics.getPendingCases().longValue() : 0L;
        long unresolvedCases = (caseMetrics != null && caseMetrics.getUnresolvedCases() != null)
                ? caseMetrics.getUnresolvedCases().longValue() : 0L;

        long upcomingMediations = caseSessionRepository.countUpcomingMediationsThisWeek(
                startOfWeek,
                endOfWeek,
                SessionType.MEDIATION,
                SessionStatus.SCHEDULED
        );

        return new BlotterDashboardKpiResponse(
                mondayDate,
                sundayDate,
                newCases,
                pendingCases,
                upcomingMediations,
                unresolvedCases
        );
    }
}
