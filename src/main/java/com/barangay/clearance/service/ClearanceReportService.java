package com.barangay.clearance.service;

import com.barangay.clearance.dto.*;
import com.barangay.clearance.model.IssuedCertificate;
import com.barangay.clearance.projection.*;
import com.barangay.clearance.repository.IssuedCertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClearanceReportService {

    private final IssuedCertificateRepository issuedCertificateRepository;
    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM d");

    /**
     * Calculates date-filtered KPI statistics for Clearance reports.
     *
     * @param startDate  Start date filter (inclusive)
     * @param endDate    End date filter (inclusive)
     * @param templateId Optional Certificate Template ID filter
     * @return {@link ClearanceReportKpiResponse}
     */
    @Transactional(readOnly = true)
    public ClearanceReportKpiResponse getReportKpis(
            LocalDate startDate,
            LocalDate endDate,
            Long templateId
    ) {
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        ClearanceReportKpiProjection kpi = issuedCertificateRepository.getClearanceReportKpiMetrics(
                startDateTime,
                endDateTime,
                templateId
        );

        long totalIssued = (kpi != null && kpi.getTotalIssued() != null) ? kpi.getTotalIssued() : 0L;
        long totalExpired = (kpi != null && kpi.getTotalExpired() != null) ? kpi.getTotalExpired() : 0L;
        long maleCount = (kpi != null && kpi.getMaleCount() != null) ? kpi.getMaleCount() : 0L;
        long femaleCount = (kpi != null && kpi.getFemaleCount() != null) ? kpi.getFemaleCount() : 0L;

        double malePct = calculatePercentage(maleCount, totalIssued);
        double femalePct = calculatePercentage(femaleCount, totalIssued);

        // Most Issued Certificate Template
        List<MostIssuedCertProjection> mostIssuedList = issuedCertificateRepository.getMostIssuedTemplate(
                startDateTime,
                endDateTime,
                templateId
        );

        String mostIssuedName = "—";
        long mostIssuedCount = 0L;

        if (mostIssuedList != null && !mostIssuedList.isEmpty()) {
            MostIssuedCertProjection top = mostIssuedList.get(0);
            mostIssuedName = top.getCertTitle() != null ? top.getCertTitle() : "Barangay Clearance";
            mostIssuedCount = top.getCount() != null ? top.getCount() : 0L;
        }

        return new ClearanceReportKpiResponse(
                startDate,
                endDate,
                templateId,
                totalIssued,
                mostIssuedName,
                mostIssuedCount,
                totalExpired,
                maleCount,
                femaleCount,
                malePct,
                femalePct
        );
    }

    /**
     * Generates daily issuance trend report with zero-fill handling.
     *
     * @param startDate  Start date
     * @param endDate    End date
     * @param templateId Optional Template filter
     * @return List of {@link ClearanceDailyTrendResponse}
     */
    @Transactional(readOnly = true)
    public List<ClearanceDailyTrendResponse> getDailyTrendReport(
            LocalDate startDate,
            LocalDate endDate,
            Long templateId
    ) {
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<ClearanceDailyTrendProjection> dbResults = issuedCertificateRepository.getDailyIssuanceCounts(
                startDateTime,
                endDateTime,
                templateId
        );

        Map<LocalDate, Long> countMap = new HashMap<>();
        if (dbResults != null) {
            for (ClearanceDailyTrendProjection p : dbResults) {
                if (p.getDate() != null) {
                    countMap.put(p.getDate(), p.getCount() != null ? p.getCount() : 0L);
                }
            }
        }

        List<ClearanceDailyTrendResponse> responseList = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            long count = countMap.getOrDefault(current, 0L);
            responseList.add(new ClearanceDailyTrendResponse(
                    current,
                    current.format(DAY_LABEL_FORMATTER),
                    current.getDayOfMonth(),
                    count
            ));
            current = current.plusDays(1);
        }

        return responseList;
    }

    /**
     * Generates monthly issuance trend report with zero-fill handling.
     *
     * @param startDate  Start date
     * @param endDate    End date
     * @param templateId Optional Template filter
     * @return List of {@link ClearanceMonthlyTrendResponse}
     */
    @Transactional(readOnly = true)
    public List<ClearanceMonthlyTrendResponse> getMonthlyTrendReport(
            LocalDate startDate,
            LocalDate endDate,
            Long templateId
    ) {
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<ClearanceMonthlyTrendProjection> dbResults = issuedCertificateRepository.getMonthlyIssuanceCounts(
                startDateTime,
                endDateTime,
                templateId
        );

        Map<YearMonth, Long> countMap = new HashMap<>();
        if (dbResults != null) {
            for (ClearanceMonthlyTrendProjection p : dbResults) {
                if (p.getYear() != null && p.getMonth() != null) {
                    countMap.put(YearMonth.of(p.getYear().intValue(), p.getMonth().intValue()), p.getCount() != null ? p.getCount() : 0L);
                }
            }
        }

        List<ClearanceMonthlyTrendResponse> responseList = new ArrayList<>();
        YearMonth current = YearMonth.from(startDate);
        YearMonth targetEnd = YearMonth.from(endDate);

        while (!current.isAfter(targetEnd)) {
            long count = countMap.getOrDefault(current, 0L);
            String monthLabel = current.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " " + current.getYear();

            responseList.add(new ClearanceMonthlyTrendResponse(
                    monthLabel,
                    current.getYear(),
                    current.getMonthValue(),
                    count
            ));
            current = current.plusMonths(1);
        }

        return responseList;
    }

    /**
     * Generates Certificate Template Type breakdown and distribution report.
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return {@link ClearanceTypeBreakdownResponse}
     */
    @Transactional(readOnly = true)
    public ClearanceTypeBreakdownResponse getTypeBreakdownReport(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<ClearanceTypeCountProjection> dbResults = issuedCertificateRepository.getTypeBreakdownCounts(
                startDateTime,
                endDateTime
        );

        long totalVolume = 0L;
        if (dbResults != null) {
            for (ClearanceTypeCountProjection p : dbResults) {
                if (p.getCount() != null) {
                    totalVolume += p.getCount();
                }
            }
        }

        List<ClearanceTypeBreakdownItem> breakdownList = new ArrayList<>();
        if (dbResults != null) {
            for (ClearanceTypeCountProjection p : dbResults) {
                long count = p.getCount() != null ? p.getCount() : 0L;
                String title = p.getCertTitle() != null ? p.getCertTitle() : "Barangay Clearance";
                double pct = calculatePercentage(count, totalVolume);

                breakdownList.add(new ClearanceTypeBreakdownItem(
                        title,
                        count,
                        pct
                ));
            }
        }

        return new ClearanceTypeBreakdownResponse(
                startDate,
                endDate,
                totalVolume,
                breakdownList
        );
    }

    /**
     * Generates Application Purpose distribution breakdown report.
     *
     * @param startDate Start date
     * @param endDate   End date
     * @return {@link ClearancePurposeBreakdownResponse}
     */
    @Transactional(readOnly = true)
    public ClearancePurposeBreakdownResponse getPurposeBreakdownReport(
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<com.barangay.clearance.model.IssuedCertificate> certs = issuedCertificateRepository.findCertificatesInDateRange(
                startDateTime,
                endDateTime
        );

        Map<String, Long> purposeCountMap = new LinkedHashMap<>();
        for (com.barangay.clearance.model.IssuedCertificate ic : certs) {
            String purpose = extractPurpose(ic);
            purposeCountMap.put(purpose, purposeCountMap.getOrDefault(purpose, 0L) + 1L);
        }

        // Sort descending by count
        List<Map.Entry<String, Long>> sortedList = purposeCountMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .toList();

        long totalVolume = certs.size();
        String highestPurpose = "—";
        long highestCount = 0L;

        if (!sortedList.isEmpty()) {
            highestPurpose = sortedList.get(0).getKey();
            highestCount = sortedList.get(0).getValue();
        }

        List<ClearancePurposeBreakdownItem> breakdownList = new ArrayList<>();
        for (Map.Entry<String, Long> entry : sortedList) {
            double pct = calculatePercentage(entry.getValue(), totalVolume);
            breakdownList.add(new ClearancePurposeBreakdownItem(
                    entry.getKey(),
                    entry.getValue(),
                    pct
            ));
        }

        return new ClearancePurposeBreakdownResponse(
                startDate,
                endDate,
                totalVolume,
                highestPurpose,
                highestCount,
                breakdownList
        );
    }

    /**
     * Calculates real-time 4 core KPIs for Clearance Operational Dashboard:
     * 1. Total Issued Today
     * 2. Total Issued This Week (Monday to Sunday)
     * 3. Total Active Clearances
     * 4. Most Issued Certificate (This Week)
     */
    @Transactional(readOnly = true)
    public ClearanceDashboardKpiResponse getDashboardKpis() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX);

        LocalDate weekStart = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
        LocalDateTime weekEndDateTime = weekEnd.atTime(LocalTime.MAX);

        // 1. Total Issued Today
        long totalIssuedToday = issuedCertificateRepository.countActiveCertificatesBetween(todayStart, todayEnd);

        // 2. Total Issued This Week
        long totalIssuedThisWeek = issuedCertificateRepository.countActiveCertificatesBetween(weekStartDateTime, weekEndDateTime);

        // 3. Total Active Clearances (All active in circulation)
        long totalActiveClearances = issuedCertificateRepository.countActiveCertificates();

        // 4. Most Issued Certificate Template This Week (with fallback to all-time active)
        List<MostIssuedCertProjection> mostIssuedList = issuedCertificateRepository.getMostIssuedTemplate(
                weekStartDateTime,
                weekEndDateTime,
                null
        );

        String mostIssuedCertThisWeek = "—";
        long mostIssuedCertCountThisWeek = 0L;

        if (mostIssuedList != null && !mostIssuedList.isEmpty()) {
            MostIssuedCertProjection top = mostIssuedList.get(0);
            mostIssuedCertThisWeek = top.getCertTitle() != null ? top.getCertTitle() : "Barangay Clearance";
            mostIssuedCertCountThisWeek = top.getCount() != null ? top.getCount() : 0L;
        } else {
            List<IssuedCertificate> allActive = issuedCertificateRepository.findAllActiveCertificates();
            if (!allActive.isEmpty()) {
                Map<String, Long> certCounts = new HashMap<>();
                for (IssuedCertificate c : allActive) {
                    String title = (c.getTemplate() != null && c.getTemplate().getCertTitle() != null)
                            ? c.getTemplate().getCertTitle()
                            : "Barangay Clearance";
                    certCounts.merge(title, 1L, Long::sum);
                }
                var topEntry = certCounts.entrySet().stream().max(Map.Entry.comparingByValue());
                if (topEntry.isPresent()) {
                    mostIssuedCertThisWeek = topEntry.get().getKey();
                    mostIssuedCertCountThisWeek = topEntry.get().getValue();
                }
            }
        }

        return new ClearanceDashboardKpiResponse(
                today,
                weekStart,
                weekEnd,
                totalIssuedToday,
                totalIssuedThisWeek,
                totalActiveClearances,
                mostIssuedCertThisWeek,
                mostIssuedCertCountThisWeek
        );
    }

    private String extractPurpose(com.barangay.clearance.model.IssuedCertificate ic) {
        String purpose = null;
        com.fasterxml.jackson.databind.JsonNode fields = ic.getFieldValues();
        if (fields != null && fields.hasNonNull("purpose")) {
            purpose = fields.get("purpose").asText();
        }
        if ((purpose == null || purpose.trim().isEmpty() || purpose.equalsIgnoreCase("Others") || purpose.equalsIgnoreCase("OTHER"))
                && fields != null && fields.hasNonNull("customPurpose")) {
            purpose = fields.get("customPurpose").asText();
        }
        if ((purpose == null || purpose.trim().isEmpty()) && ic.getArchiveRemarks() != null) {
            purpose = ic.getArchiveRemarks();
        }
        if (purpose == null || purpose.trim().isEmpty()) {
            return "General Purpose";
        }
        return purpose.trim();
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
}
