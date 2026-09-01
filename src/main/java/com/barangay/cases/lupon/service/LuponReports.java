package com.barangay.cases.lupon.service;

import com.barangay.cases.blotter.dto.DailyCaseTrendResponse;
import com.barangay.cases.blotter.dto.MonthlyCaseTrendResponse;
import com.barangay.cases.lupon.dto.LuponDashboardKpiResponse;
import com.barangay.cases.lupon.dto.LuponReportKpiResponse;
import com.barangay.cases.lupon.projection.LuponDashboardKpiProjection;
import com.barangay.cases.shared.projection.CaseCountByDayProjection;
import com.barangay.cases.shared.projection.CaseCountByMonthProjection;
import com.barangay.cases.shared.projection.LuponReportKpiProjection;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.repository.CaseSessionRepository;
import com.barangay.zshared.enumerated.SessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
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
    private final CaseSessionRepository caseSessionRepository;

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

    /**
     * Calculates the 4 core Lupon Operational Dashboard KPI metrics:
     * 1. Total Cases Referred to Lupon this week (Monday to Sunday)
     * 2. Active cases currently under conciliation
     * 3. Upcoming Scheduled Conciliation Hearings this week
     * 4. Total Certificate to File Action (CFA) issued this week
     *
     * Excludes archived, soft-deleted, and permanently deleted cases.
     * Deduplicates child cycles appropriately.
     *
     * @return {@link LuponDashboardKpiResponse}
     */
    @Transactional(readOnly = true)
    public LuponDashboardKpiResponse getDashboardKpis() {
        LocalDate today = LocalDate.now();
        LocalDate mondayDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sundayDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        LocalDateTime startOfWeek = mondayDate.atStartOfDay();
        LocalDateTime endOfWeek = sundayDate.atTime(LocalTime.MAX);

        LuponDashboardKpiProjection caseMetrics = caseRepository.getLuponDashboardCaseKpis(
                startOfWeek,
                endOfWeek
        );

        long totalReferred = (caseMetrics != null && caseMetrics.getTotalReferredThisWeek() != null)
                ? caseMetrics.getTotalReferredThisWeek().longValue() : 0L;
        long totalUnderConciliation = (caseMetrics != null && caseMetrics.getTotalUnderConciliation() != null)
                ? caseMetrics.getTotalUnderConciliation().longValue() : 0L;
        long totalCfaIssued = (caseMetrics != null && caseMetrics.getTotalCfaIssuedThisWeek() != null)
                ? caseMetrics.getTotalCfaIssuedThisWeek().longValue() : 0L;

        long upcomingHearings = caseSessionRepository.countUpcomingLuponSessionsThisWeek(
                startOfWeek,
                endOfWeek,
                SessionStatus.SCHEDULED
        );

        return new LuponDashboardKpiResponse(
                mondayDate,
                sundayDate,
                totalReferred,
                totalUnderConciliation,
                upcomingHearings,
                totalCfaIssued
        );
    }

    /**
     * Retrieves up to 5 of the most recently referred Lupon cases processed today.
     * Deduplicates reopened cycles to show only distinct root cases with their endmost child case status.
     *
     * @return list of recent cases (empty if none, up to max 5)
     */
    @Transactional(readOnly = true)
    public List<com.barangay.cases.blotter.dto.RecentBlotterCaseResponse> getRecentCasesToday() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<com.barangay.cases.shared.model.Case> casesToday = caseRepository.findRecentLuponCasesToday(
                startOfDay,
                endOfDay
        );

        Map<Long, com.barangay.cases.blotter.dto.RecentBlotterCaseResponse> distinctRootCasesMap = new LinkedHashMap<>();

        for (com.barangay.cases.shared.model.Case c : casesToday) {
            com.barangay.cases.shared.model.Case rootCase = findRootCase(c);
            if (distinctRootCasesMap.containsKey(rootCase.getId())) {
                continue;
            }

            List<com.barangay.cases.shared.model.Case> lineage = getCaseLineage(rootCase);
            com.barangay.cases.shared.model.Case endmostCase = lineage.get(lineage.size() - 1);

            String complainantName = "—";
            if (rootCase.getComplainant() != null && rootCase.getComplainant().getPerson() != null) {
                String fn = rootCase.getComplainant().getPerson().getFirstName() != null ? rootCase.getComplainant().getPerson().getFirstName() : "";
                String ln = rootCase.getComplainant().getPerson().getLastName() != null ? rootCase.getComplainant().getPerson().getLastName() : "";
                complainantName = (fn + " " + ln).trim();
                if (complainantName.isEmpty()) complainantName = "—";
            }

            String respondentName = "—";
            if (rootCase.getRespondent() != null && rootCase.getRespondent().getPerson() != null) {
                String fn = rootCase.getRespondent().getPerson().getFirstName() != null ? rootCase.getRespondent().getPerson().getFirstName() : "";
                String ln = rootCase.getRespondent().getPerson().getLastName() != null ? rootCase.getRespondent().getPerson().getLastName() : "";
                respondentName = (fn + " " + ln).trim();
                if (respondentName.isEmpty()) respondentName = "—";
            }

            com.barangay.cases.blotter.dto.RecentBlotterCaseResponse resp = new com.barangay.cases.blotter.dto.RecentBlotterCaseResponse(
                    rootCase.getId(),
                    rootCase.getCaseNumber(),
                    complainantName,
                    respondentName,
                    rootCase.getFilingType(),
                    endmostCase.getCaseStatus(),
                    rootCase.getCreatedAt()
            );

            distinctRootCasesMap.put(rootCase.getId(), resp);

            if (distinctRootCasesMap.size() >= 5) {
                break;
            }
        }

        return new ArrayList<>(distinctRootCasesMap.values());
    }

    /**
     * Retrieves up to 5 of today's scheduled conciliation hearings for Lupon cases.
     *
     * @return list of {@link com.barangay.cases.blotter.dto.QuickMediationSummaryDto}
     */
    @Transactional(readOnly = true)
    public List<com.barangay.cases.blotter.dto.QuickMediationSummaryDto> getQuickTodayConciliations() {
        LocalDate today = LocalDate.now();
        return caseSessionRepository.findQuickTodayLuponConciliations(
                today.atStartOfDay(),
                today.atTime(LocalTime.MAX),
                org.springframework.data.domain.PageRequest.of(0, 5)
        );
    }

    private com.barangay.cases.shared.model.Case findRootCase(com.barangay.cases.shared.model.Case targetCase) {
        com.barangay.cases.shared.model.Case root = targetCase;
        while (root.getPreviousCaseNumber() != null && !root.getPreviousCaseNumber().trim().isEmpty()) {
            Optional<com.barangay.cases.shared.model.Case> prevOpt = caseRepository.findByCaseNumber(root.getPreviousCaseNumber().trim());
            if (prevOpt.isPresent()) {
                root = prevOpt.get();
            } else {
                break;
            }
        }
        return root;
    }

    private List<com.barangay.cases.shared.model.Case> getCaseLineage(com.barangay.cases.shared.model.Case rootCase) {
        List<com.barangay.cases.shared.model.Case> lineage = new ArrayList<>();
        com.barangay.cases.shared.model.Case curr = rootCase;
        lineage.add(curr);
        while (curr.getReOpenedCaseNumber() != null && !curr.getReOpenedCaseNumber().trim().isEmpty()) {
            Optional<com.barangay.cases.shared.model.Case> nextOpt = caseRepository.findByCaseNumber(curr.getReOpenedCaseNumber().trim());
            if (nextOpt.isPresent()) {
                curr = nextOpt.get();
                lineage.add(curr);
            } else {
                break;
            }
        }
        return lineage;
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
