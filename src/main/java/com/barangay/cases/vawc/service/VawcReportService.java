package com.barangay.cases.vawc.service;

import com.barangay.cases.blotter.dto.CaseStatusBreakdownResponse;
import com.barangay.cases.blotter.dto.CaseStatusItemDto;
import com.barangay.cases.blotter.dto.DailyCaseTrendResponse;
import com.barangay.cases.blotter.dto.MonthlyCaseTrendResponse;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.projection.CaseCountByDayProjection;
import com.barangay.cases.shared.projection.CaseCountByMonthProjection;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.vawc.dto.VawcAgencyReferralBreakdownResponse;
import com.barangay.cases.vawc.dto.VawcReportKpiResponse;
import com.barangay.cases.vawc.dto.VawcTableRowResponse;
import com.barangay.cases.vawc.model.ReferralFeedback;
import com.barangay.cases.vawc.model.VawcReferral;
import com.barangay.cases.vawc.dto.VawcDashboardKpiResponse;
import com.barangay.cases.vawc.repository.BarangayProtectionOrderRepository;
import com.barangay.cases.vawc.repository.ReferralFeedbackRepository;
import com.barangay.cases.vawc.repository.VawcReferralRepository;
import com.barangay.zshared.enumerated.BpoStatus;
import com.barangay.zshared.enumerated.CaseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for generating date-filtered VAWC Reports and Statistical Analytics.
 * Handles calculation of:
 * 1. KPI Summary Cards (Total Cases, Total Referred, Top Successful Partner Agency, Total Dismissed)
 * 2. Inter-Agency Referral Distribution & Resolution Breakdown
 * 3. Monthly & Daily Case Inflow Trends
 * 4. Status Breakdown Analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VawcReportService {

    private final CaseRepository caseRepository;
    private final VawcReferralRepository vawcReferralRepository;
    private final ReferralFeedbackRepository referralFeedbackRepository;
    private final BarangayProtectionOrderRepository barangayProtectionOrderRepository;
    private final VawcCaseService vawcCaseService;

    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM d");

    /**
     * Generates real-time Dashboard KPIs for VAWC Desk.
     * Computes weekly metrics starting Monday to Sunday:
     * 1. Total VAWC cases filed this week (with trend vs previous Monday-Sunday week)
     * 2. Total active BPOs this week
     * 3. Total referred cases this week
     * 4. Total withdrawn cases this week
     *
     * @return {@link VawcDashboardKpiResponse}
     */
    @Transactional(readOnly = true)
    public VawcDashboardKpiResponse getDashboardKpis() {
        LocalDate today = LocalDate.now();
        LocalDate mondayDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sundayDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        LocalDateTime startOfWeek = mondayDate.atStartOfDay();
        LocalDateTime endOfWeek = sundayDate.atTime(LocalTime.MAX);

        // Previous week Monday to Sunday
        LocalDateTime prevStartOfWeek = mondayDate.minusWeeks(1).atStartOfDay();
        LocalDateTime prevEndOfWeek = sundayDate.minusWeeks(1).atTime(LocalTime.MAX);

        // 1. Total Cases This Week & Trend vs Previous Week
        long totalCasesThisWeek = caseRepository.countRootVawcCasesByCreatedAtBetween(startOfWeek, endOfWeek);
        long prevTotalCases = caseRepository.countRootVawcCasesByCreatedAtBetween(prevStartOfWeek, prevEndOfWeek);
        String casesTrend = calculateTrend(totalCasesThisWeek, prevTotalCases);

        // 2. Total Active BPO This Week (ISSUED, SERVED)
        List<BpoStatus> activeBpoStatuses = List.of(BpoStatus.ISSUED, BpoStatus.SERVED);
        long totalActiveBpoThisWeek = barangayProtectionOrderRepository.countActiveBpoBetween(
                activeBpoStatuses, startOfWeek, endOfWeek
        );

        // 3. Total Referred Cases This Week
        long totalReferredCasesThisWeek = vawcReferralRepository.countReferralsBetween(startOfWeek, endOfWeek);

        // 4. Total Withdrawn Cases This Week
        long totalWithdrawnCasesThisWeek = caseRepository.countVawcCasesByCaseStatusInBetween(
                List.of(CaseStatus.WITHDRAWN), startOfWeek, endOfWeek
        );

        return new VawcDashboardKpiResponse(
                mondayDate,
                sundayDate,
                totalCasesThisWeek,
                casesTrend,
                totalActiveBpoThisWeek,
                totalReferredCasesThisWeek,
                totalWithdrawnCasesThisWeek
        );
    }

    /**
     * Calculates date-filtered KPI metrics for VAWC department reports:
     * 1. Total VAWC Cases in period (with previous period comparison trend)
     * 2. Total Referred Cases in period
     * 3. Most Agency Success Referred (Top partner agency with highest resolutions)
     * 4. Total Dismissed Cases in period
     *
     * @param startDate the inclusive starting date
     * @param endDate   the inclusive ending date
     * @return {@link VawcReportKpiResponse}
     */
    @Transactional(readOnly = true)
    public VawcReportKpiResponse getReportKpi(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusMonths(6);
        if (endDate == null) endDate = LocalDate.now();

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Previous period of identical duration for accurate percentage trend comparison
        long daysInPeriod = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDateTime prevStartDateTime = startDate.minusDays(daysInPeriod).atStartOfDay();
        LocalDateTime prevEndDateTime = startDate.minusDays(1).atTime(LocalTime.MAX);

        // 1. Total Cases
        long currentTotalCases = caseRepository.countRootVawcCasesByCreatedAtBetween(startDateTime, endDateTime);
        long prevTotalCases = caseRepository.countRootVawcCasesByCreatedAtBetween(prevStartDateTime, prevEndDateTime);
        String casesTrend = calculateTrend(currentTotalCases, prevTotalCases);

        // 2. Total Referred Cases
        long currentReferred = vawcReferralRepository.countReferralsBetween(startDateTime, endDateTime);
        long prevReferred = vawcReferralRepository.countReferralsBetween(prevStartDateTime, prevEndDateTime);
        String referredTrend = calculateTrend(currentReferred, prevReferred);

        // 3. Total Dismissed Cases
        long currentDismissed = caseRepository.countVawcCasesByCaseStatusInBetween(
                List.of(CaseStatus.DISMISSED), startDateTime, endDateTime);
        long prevDismissed = caseRepository.countVawcCasesByCaseStatusInBetween(
                List.of(CaseStatus.DISMISSED), prevStartDateTime, prevEndDateTime);
        String dismissedTrend = calculateTrend(currentDismissed, prevDismissed);

        // 4. Most Agency Success Referred
        List<VawcReferral> referrals = vawcReferralRepository.findReferralsBetween(startDateTime, endDateTime);
        String topAgencyName = "None Yet";
        long topAgencySuccessCount = 0;
        double topAgencySuccessRate = 0.0;

        if (!referrals.isEmpty()) {
            Map<String, List<VawcReferral>> agencyMap = referrals.stream()
                    .collect(Collectors.groupingBy(r -> normalizeAgencyName(r.getReferredTo())));

            String bestAgency = null;
            long maxResolved = -1;
            double bestRate = 0.0;
            long bestTotal = 0;

            for (Map.Entry<String, List<VawcReferral>> entry : agencyMap.entrySet()) {
                String agency = entry.getKey();
                List<VawcReferral> agencyRefs = entry.getValue();
                long totalForAgency = agencyRefs.size();
                long resolvedForAgency = agencyRefs.stream().filter(this::isReferralResolved).count();

                double rate = totalForAgency > 0
                        ? BigDecimal.valueOf((resolvedForAgency * 100.0) / totalForAgency)
                        .setScale(1, RoundingMode.HALF_UP).doubleValue()
                        : 0.0;

                // Priority: Highest resolved count, then highest rate
                if (resolvedForAgency > maxResolved || (resolvedForAgency == maxResolved && totalForAgency > bestTotal)) {
                    maxResolved = resolvedForAgency;
                    bestAgency = agency;
                    bestRate = rate;
                    bestTotal = totalForAgency;
                }
            }

            if (bestAgency != null) {
                topAgencyName = bestAgency;
                topAgencySuccessCount = maxResolved > 0 ? maxResolved : bestTotal;
                topAgencySuccessRate = bestRate;
            }
        }

        return new VawcReportKpiResponse(
                currentTotalCases,
                currentReferred,
                topAgencyName,
                topAgencySuccessCount,
                topAgencySuccessRate,
                currentDismissed,
                casesTrend,
                referredTrend,
                dismissedTrend
        );
    }

    /**
     * Generates a distribution breakdown report of inter-agency referrals by partner agency.
     *
     * @param startDate the inclusive start date
     * @param endDate   the inclusive end date
     * @return list of {@link VawcAgencyReferralBreakdownResponse}
     */
    @Transactional(readOnly = true)
    public List<VawcAgencyReferralBreakdownResponse> getAgencyReferralBreakdown(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusMonths(6);
        if (endDate == null) endDate = LocalDate.now();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<VawcReferral> referrals = vawcReferralRepository.findReferralsBetween(startDateTime, endDateTime);
        long grandTotal = referrals.size();

        if (referrals.isEmpty()) {
            return List.of();
        }

        Map<String, List<VawcReferral>> grouped = referrals.stream()
                .collect(Collectors.groupingBy(r -> normalizeAgencyName(r.getReferredTo())));

        List<VawcAgencyReferralBreakdownResponse> result = new ArrayList<>();

        for (Map.Entry<String, List<VawcReferral>> entry : grouped.entrySet()) {
            String agency = entry.getKey();
            List<VawcReferral> agencyRefs = entry.getValue();

            long totalRefs = agencyRefs.size();
            long resolved = agencyRefs.stream().filter(this::isReferralResolved).count();
            long declined = agencyRefs.stream().filter(this::isReferralDeclined).count();
            long pending = totalRefs - resolved - declined;
            if (pending < 0) pending = 0;

            double pct = grandTotal > 0
                    ? BigDecimal.valueOf((totalRefs * 100.0) / grandTotal)
                    .setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            double successRate = totalRefs > 0
                    ? BigDecimal.valueOf((resolved * 100.0) / totalRefs)
                    .setScale(1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            result.add(new VawcAgencyReferralBreakdownResponse(
                    agency,
                    totalRefs,
                    pct,
                    resolved,
                    declined,
                    pending,
                    successRate
            ));
        }

        // Sort descending by total referrals
        result.sort(Comparator.comparingLong(VawcAgencyReferralBreakdownResponse::totalReferrals).reversed());
        return result;
    }

    /**
     * Generates a monthly case inflow trend report for VAWC cases within a date range.
     *
     * @param startDate the inclusive starting date
     * @param endDate   the inclusive ending date
     * @return list of {@link MonthlyCaseTrendResponse}
     */
    @Transactional(readOnly = true)
    public List<MonthlyCaseTrendResponse> getMonthlyTrendReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusMonths(6);
        if (endDate == null) endDate = LocalDate.now();

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<CaseCountByMonthProjection> dbResult = caseRepository.getVawcMonthlyCaseCounts(startDateTime, endDateTime);

        Map<YearMonth, Long> countMap = dbResult != null ? dbResult.stream().collect(
                Collectors.toMap(
                        p -> YearMonth.of(p.getYear(), p.getMonth()),
                        CaseCountByMonthProjection::getCount,
                        (a, b) -> a
                )
        ) : Map.of();

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
     * Generates a daily case inflow trend report for VAWC cases within a date range.
     *
     * @param startDate the inclusive starting date
     * @param endDate   the inclusive ending date
     * @return list of {@link DailyCaseTrendResponse}
     */
    @Transactional(readOnly = true)
    public List<DailyCaseTrendResponse> getDailyTrendReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusDays(30);
        if (endDate == null) endDate = LocalDate.now();

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<CaseCountByDayProjection> dbResults = caseRepository.getVawcDailyCaseCounts(startDateTime, endDateTime);

        Map<LocalDate, Long> countMap = new HashMap<>();
        if (dbResults != null) {
            for (CaseCountByDayProjection p : dbResults) {
                if (p.getDate() != null) {
                    countMap.put(p.getDate(), p.getCount());
                }
            }
        }

        List<DailyCaseTrendResponse> responseList = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            long count = countMap.getOrDefault(current, 0L);
            String label = current.format(DAY_LABEL_FORMATTER);

            responseList.add(new DailyCaseTrendResponse(
                    current,
                    label,
                    current.getDayOfMonth(),
                    count
            ));

            current = current.plusDays(1);
        }

        return responseList;
    }

    /**
     * Generates a comprehensive status breakdown report for all VAWC cases within a date range.
     *
     * @param startDate the inclusive starting date
     * @param endDate   the inclusive ending date
     * @return {@link CaseStatusBreakdownResponse}
     */
    @Transactional(readOnly = true)
    public CaseStatusBreakdownResponse getStatusBreakdownReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusMonths(6);
        if (endDate == null) endDate = LocalDate.now();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Case> cases = caseRepository.findRootVawcCasesBetween(startDateTime, endDateTime);
        long totalCases = cases.size();

        Map<CaseStatus, Long> statusCountMap = cases.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCaseStatus() != null ? c.getCaseStatus() : CaseStatus.PENDING,
                        Collectors.counting()
                ));

        List<CaseStatusItemDto> breakdown = new ArrayList<>();

        for (CaseStatus status : CaseStatus.values()) {
            long count = statusCountMap.getOrDefault(status, 0L);
            if (count > 0 || isCommonVawcStatus(status)) {
                double pct = totalCases > 0
                        ? BigDecimal.valueOf((count * 100.0) / totalCases)
                        .setScale(1, RoundingMode.HALF_UP).doubleValue()
                        : 0.0;

                breakdown.add(new CaseStatusItemDto(
                        status,
                        formatStatusLabel(status),
                        count,
                        pct
                ));
            }
        }

        // Sort descending by count
        breakdown.sort(Comparator.comparingLong(CaseStatusItemDto::count).reversed());

        return new CaseStatusBreakdownResponse(
                startDate,
                endDate,
                null,
                totalCases,
                breakdown
        );
    }

    /**
     * Retrieves all VAWC table records within a date range for export/table preview.
     */
    @Transactional(readOnly = true)
    public List<VawcTableRowResponse> getVawcReportRecords(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusMonths(6);
        if (endDate == null) endDate = LocalDate.now();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Case> cases = caseRepository.findRootVawcCasesBetween(startDateTime, endDateTime);
        return cases.stream().map(this::mapToTableRow).filter(Objects::nonNull).toList();
    }

    private VawcTableRowResponse mapToTableRow(Case c) {
        if (c == null) return null;
        String compName = "—";
        if (c.getComplainant() != null && c.getComplainant().getPerson() != null) {
            String fn = c.getComplainant().getPerson().getFirstName() != null ? c.getComplainant().getPerson().getFirstName() : "";
            String ln = c.getComplainant().getPerson().getLastName() != null ? c.getComplainant().getPerson().getLastName() : "";
            compName = (fn + " " + ln).trim();
            if (compName.isEmpty()) compName = "Complainant";
        }

        String respName = "—";
        if (c.getRespondent() != null && c.getRespondent().getPerson() != null) {
            String fn = c.getRespondent().getPerson().getFirstName() != null ? c.getRespondent().getPerson().getFirstName() : "";
            String ln = c.getRespondent().getPerson().getLastName() != null ? c.getRespondent().getPerson().getLastName() : "";
            respName = (fn + " " + ln).trim();
            if (respName.isEmpty()) respName = "Respondent";
        }

        String nature = c.getIncidentDetail() != null && c.getIncidentDetail().getNatureOfComplaint() != null
                ? c.getIncidentDetail().getNatureOfComplaint()
                : "VAWC Incident";

        String dateFiled = c.getIncidentDetail() != null && c.getIncidentDetail().getDateOfIncident() != null
                ? c.getIncidentDetail().getDateOfIncident().toString()
                : (c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate().toString() : "—");

        return new VawcTableRowResponse(
                c.getId(),
                c.getCaseNumber(),
                compName,
                respName,
                nature,
                c.getCaseStatus(),
                c.getRiskLevel(),
                dateFiled,
                c.getCreatedAt()
        );
    }

    // ==========================================
    // PRIVATE HELPER METHODS
    // ==========================================

    private boolean isReferralResolved(VawcReferral r) {
        if (r == null) return false;
        if ("Resolved".equalsIgnoreCase(r.getStatus())) return true;
        if (r.getId() != null) {
            ReferralFeedback fb = referralFeedbackRepository.findByVawcReferralId(r.getId()).orElse(null);
            return fb != null && Boolean.TRUE.equals(fb.getResolvesCase());
        }
        return false;
    }

    private boolean isReferralDeclined(VawcReferral r) {
        if (r == null) return false;
        if ("Declined".equalsIgnoreCase(r.getStatus())) return true;
        if (r.getId() != null) {
            ReferralFeedback fb = referralFeedbackRepository.findByVawcReferralId(r.getId()).orElse(null);
            return fb != null && Boolean.TRUE.equals(fb.getDeclinedByAgency());
        }
        return false;
    }

    private String normalizeAgencyName(String raw) {
        if (raw == null || raw.isBlank()) return "CSWD / DSWD";
        String upper = raw.toUpperCase().trim();
        if (upper.contains("CSWD") || upper.contains("DSWD") || upper.contains("SOCIAL WELFARE")) {
            return "CSWD / DSWD";
        }
        if (upper.contains("PNP") || upper.contains("POLICE") || upper.contains("WCPD") || upper.contains("WOMEN AND CHILDREN")) {
            return "PNP - WCPD";
        }
        if (upper.contains("PAO") || upper.contains("PUBLIC ATTORNEY") || upper.contains("LEGAL")) {
            return "Public Attorney's Office (PAO)";
        }
        if (upper.contains("HOSPITAL") || upper.contains("HEALTH") || upper.contains("MEDICAL") || upper.contains("CLINIC")) {
            return "City Health / Medical Hospital";
        }
        return raw.trim();
    }

    private String calculateTrend(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? "+100.0% vs previous period" : "0.0% vs previous period";
        }
        double change = ((double) (current - previous) / previous) * 100.0;
        BigDecimal rounded = BigDecimal.valueOf(change).setScale(1, RoundingMode.HALF_UP);
        String prefix = change > 0 ? "+" : "";
        return prefix + rounded + "% vs previous period";
    }

    private boolean isCommonVawcStatus(CaseStatus s) {
        return s == CaseStatus.PENDING ||
                s == CaseStatus.BPO_ISSUED ||
                s == CaseStatus.BPO_SERVED ||
                s == CaseStatus.ISSUED_REFERRAL ||
                s == CaseStatus.SETTLED ||
                s == CaseStatus.DISMISSED;
    }

    private String formatStatusLabel(CaseStatus status) {
        if (status == null) return "Unknown";
        return switch (status) {
            case PENDING -> "Pending Assessment";
            case BPO_ISSUED -> "BPO Issued";
            case BPO_SERVED -> "BPO Served";
            case ISSUED_REFERRAL -> "Referred to Agency";
            case DECLINED_REFERRAL -> "Declined by Agency";
            case SETTLED -> "Settled / Resolved";
            case DISMISSED -> "Dismissed";
            case WITHDRAWN -> "Withdrawn";
            case EXPIRED_UNACTIONED -> "Expired / Unactioned (15-Day)";
            default -> status.name().replace("_", " ");
        };
    }
}
