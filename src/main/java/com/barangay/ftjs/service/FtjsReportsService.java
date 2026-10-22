package com.barangay.ftjs.service;

import com.barangay.ftjs.dto.*;
import com.barangay.ftjs.projection.AgeRangeDistributionProjection;
import com.barangay.ftjs.projection.EducationDistributionProjection;
import com.barangay.ftjs.projection.FtjsReportTableProjection;
import com.barangay.ftjs.projection.IssuanceTypeProjection;
import com.barangay.ftjs.projection.PurposeDistributionProjection;
import com.barangay.ftjs.projection.TrendPointProjection;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.barangay.zshared.enumerated.FtjsStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * Service class generating adaptive analytics, trend reports, and demographic distributions for FTJS.
 */
public class FtjsReportsService {

    private final FirstTimeJobSeekerRepository ftjsRepository;

    /**
     * Generates adaptive monthly or granular trend reports based on date filter.
     */
    public TrendReportResponse generateAdaptiveTrendReport(FtjsReportFilterDto filter) {
        LocalDate rawEnd = filter.dateTo() != null ? filter.dateTo() : LocalDate.now();
        LocalDate rawStart = filter.dateFrom() != null ? filter.dateFrom() : rawEnd.minusMonths(6);

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
                normalizeStatus(filter.status()),
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

    /**
     * Computes education attainment breakdown among FTJS applicants.
     */
    public EducationDistributionResponse generateEducationDistribution(FtjsReportFilterDto filter) {
        LocalDate rawEnd = filter.dateTo() != null ? filter.dateTo() : LocalDate.now();
        LocalDate rawStart = filter.dateFrom() != null ? filter.dateFrom() : rawEnd.minusMonths(6);

        // Date Inversion Guard
        LocalDate start = rawStart.isAfter(rawEnd) ? rawEnd : rawStart;
        LocalDate end = rawStart.isAfter(rawEnd) ? rawStart : rawEnd;

        List<EducationDistributionProjection> results = ftjsRepository.getEducationDistribution(
                start, end,
                normalize(filter.purpose()),
                normalize(filter.sex()),
                normalizeStatus(filter.status()),
                normalize(filter.educationalAttainment())
        );

        List<EducationDistributionPointDto> points = new ArrayList<>();
        String highestLabel = null;
        long highestCount = 0L;
        long totalVolume = 0L;

        for (EducationDistributionProjection record : results) {
            long count = record.getApplicantCount() != null ? record.getApplicantCount() : 0L;
            points.add(new EducationDistributionPointDto(record.getEducationalAttainment(), count));

            totalVolume += count;
            if (count > highestCount) {
                highestCount = count;
                highestLabel = record.getEducationalAttainment();
            }
        }

        return new EducationDistributionResponse(points, highestLabel, highestCount, totalVolume);
    }

    /**
     * Computes purpose breakdown for FTJS certificate applications.
     */
    public PurposeDistributionResponse generatePurposeDistribution(FtjsReportFilterDto filter) {
        LocalDate rawEnd = filter.dateTo() != null ? filter.dateTo() : LocalDate.now();
        LocalDate rawStart = filter.dateFrom() != null ? filter.dateFrom() : rawEnd.minusMonths(6);

        LocalDate start = rawStart.isAfter(rawEnd) ? rawEnd : rawStart;
        LocalDate end = rawStart.isAfter(rawEnd) ? rawStart : rawEnd;

        List<PurposeDistributionProjection> results = ftjsRepository.getPurposeDistribution(
                start, end,
                normalize(filter.purpose()),
                normalize(filter.sex()),
                normalizeStatus(filter.status()),
                normalize(filter.educationalAttainment())
        );

        long totalVolume = results.stream()
                .mapToLong(r -> r.getApplicantCount() != null ? r.getApplicantCount() : 0L)
                .sum();

        List<PurposeDistributionPointDto> points = new ArrayList<>();
        for (PurposeDistributionProjection record : results) {
            long count = record.getApplicantCount() != null ? record.getApplicantCount() : 0L;
            double share = totalVolume > 0 ? Math.round(((double) count * 100.0 / totalVolume) * 10.0) / 10.0 : 0.0;
            points.add(new PurposeDistributionPointDto(record.getPurpose(), count, share));
        }

        return new PurposeDistributionResponse(points, totalVolume);
    }

    public IssuanceTypeDistributionResponse generateIssuanceTypeDistribution(FtjsReportFilterDto filter) {
        LocalDate rawEnd = filter.dateTo() != null ? filter.dateTo() : LocalDate.now();
        LocalDate rawStart = filter.dateFrom() != null ? filter.dateFrom() : rawEnd.minusMonths(6);

        LocalDate start = rawStart.isAfter(rawEnd) ? rawEnd : rawStart;
        LocalDate end = rawStart.isAfter(rawEnd) ? rawStart : rawEnd;

        List<IssuanceTypeProjection> results = ftjsRepository.getIssuanceTypeDistribution(
                start, end,
                normalize(filter.purpose()),
                normalize(filter.sex()),
                normalizeStatus(filter.status()),
                normalize(filter.educationalAttainment())
        );

        long totalVolume = results.stream()
                .mapToLong(r -> r.getApplicantCount() != null ? r.getApplicantCount() : 0L)
                .sum();

        List<IssuanceTypePointDto> points = new ArrayList<>();
        for (IssuanceTypeProjection record : results) {
            long count = record.getApplicantCount() != null ? record.getApplicantCount() : 0L;
            double share = totalVolume > 0 ? Math.round(((double) count * 100.0 / totalVolume) * 10.0) / 10.0 : 0.0;
            points.add(new IssuanceTypePointDto(record.getIssuanceType(), count, share));
        }

        return new IssuanceTypeDistributionResponse(points, totalVolume);
    }

    public Page<FtjsReportTableResponse> generateReportTable(
            FtjsReportFilterDto filter,
            String search,
            Pageable pageable) {

        LocalDate rawEnd = filter.dateTo() != null ? filter.dateTo() : LocalDate.now();
        LocalDate rawStart = filter.dateFrom() != null ? filter.dateFrom() : rawEnd.minusMonths(6);

        LocalDate start = rawStart.isAfter(rawEnd) ? rawEnd : rawStart;
        LocalDate end = rawStart.isAfter(rawEnd) ? rawStart : rawEnd;

        Page<FtjsReportTableProjection> projections = ftjsRepository.findReportTableFiltered(
                start,
                end,
                normalizeStatus(filter.status()),
                normalize(filter.purpose()),
                normalize(filter.sex()),
                normalize(filter.educationalAttainment()),
                (search != null && !search.trim().isEmpty()) ? search.trim() : null,
                pageable
        );

        return projections.map(p -> {
            FtjsStatus statusEnum = null;
            if (p.getStatus() != null) {
                try {
                    statusEnum = FtjsStatus.valueOf(p.getStatus().trim().toUpperCase().replace("-", "_").replace(" ", "_"));
                } catch (IllegalArgumentException e) {
                    // fallback
                }
            }
            return new FtjsReportTableResponse(
                    p.getId(),
                    p.getApplicationNo(),
                    p.getApplicantName(),
                    p.getIssuanceCount(),
                    p.getPurpose(),
                    statusEnum,
                    p.getDateSubmitted(),
                    p.getGender(),
                    p.getAge(),
                    p.getEducationalAttainment(),
                    p.getInstitution(),
                    p.getReleasedBy()
            );
        });
    }

    /**
     * Calculates key performance indicators for FTJS reporting dashboards.
     */
    public FtjsReportKpiResponse generateReportKpi(FtjsReportFilterDto filter) {
        LocalDate rawEnd = filter.dateTo() != null ? filter.dateTo() : LocalDate.now();
        LocalDate rawStart = filter.dateFrom() != null ? filter.dateFrom() : rawEnd.minusMonths(6);

        LocalDate start = rawStart.isAfter(rawEnd) ? rawEnd : rawStart;
        LocalDate end = rawStart.isAfter(rawEnd) ? rawStart : rawEnd;

        String normPurpose = normalize(filter.purpose());
        String normSex = normalize(filter.sex());
        String normStatus = normalizeStatus(filter.status());
        String normEducation = normalize(filter.educationalAttainment());

        // Card 1: Total Records
        List<IssuanceTypeProjection> issuanceResults = ftjsRepository.getIssuanceTypeDistribution(
                start, end, normPurpose, normSex, normStatus, normEducation
        );

        long totalRecords = issuanceResults.stream()
                .mapToLong(r -> r.getApplicantCount() != null ? r.getApplicantCount() : 0L)
                .sum();
        String totalRecordsSubtitle = "Issued and re-issued certificates within range";

        // Card 2: First-Time Issuances
        long firstTimeIssuances = issuanceResults.stream()
                .filter(r -> "Original issuance".equalsIgnoreCase(r.getIssuanceType()))
                .mapToLong(r -> r.getApplicantCount() != null ? r.getApplicantCount() : 0L)
                .sum();

        double firstTimeShare = totalRecords > 0 ? Math.round(((double) firstTimeIssuances * 100.0 / totalRecords) * 10.0) / 10.0 : 0.0;
        String firstTimeSubtitle = String.format("%.0f%% of records are first-time requests", firstTimeShare);

        // Card 3: Most Common Purpose
        List<PurposeDistributionProjection> purposeResults = ftjsRepository.getPurposeDistribution(
                start, end, normPurpose, normSex, normStatus, normEducation
        );

        String mostCommonPurpose = "N/A";
        double mostCommonPurposeShare = 0.0;
        String mostCommonPurposeSubtitle = "N/A";

        if (!purposeResults.isEmpty()) {
            PurposeDistributionProjection topPurpose = purposeResults.get(0);
            mostCommonPurpose = topPurpose.getPurpose() != null ? topPurpose.getPurpose() : "N/A";
            long topPurposeCount = topPurpose.getApplicantCount() != null ? topPurpose.getApplicantCount() : 0L;
            mostCommonPurposeShare = totalRecords > 0 ? Math.round(((double) topPurposeCount * 100.0 / totalRecords) * 10.0) / 10.0 : 0.0;
            mostCommonPurposeSubtitle = String.format("%s — %.0f%%", mostCommonPurpose, mostCommonPurposeShare);
        }

        // Card 4: Most Common Age Range
        List<AgeRangeDistributionProjection> ageResults = ftjsRepository.getAgeRangeDistribution(
                start, end, normPurpose, normSex, normStatus, normEducation
        );

        String mostCommonAgeRange = "N/A";
        double mostCommonAgeRangeShare = 0.0;
        String mostCommonAgeRangeSubtitle = "N/A";

        if (!ageResults.isEmpty()) {
            AgeRangeDistributionProjection topAge = ageResults.get(0);
            mostCommonAgeRange = topAge.getAgeRange() != null ? topAge.getAgeRange() : "N/A";
            long topAgeCount = topAge.getApplicantCount() != null ? topAge.getApplicantCount() : 0L;
            mostCommonAgeRangeShare = totalRecords > 0 ? Math.round(((double) topAgeCount * 100.0 / totalRecords) * 10.0) / 10.0 : 0.0;
            mostCommonAgeRangeSubtitle = String.format("%s years old — %.0f%%", mostCommonAgeRange, mostCommonAgeRangeShare);
        }

        return new FtjsReportKpiResponse(
                totalRecords,
                totalRecordsSubtitle,
                firstTimeIssuances,
                firstTimeShare,
                firstTimeSubtitle,
                mostCommonPurpose,
                mostCommonPurposeShare,
                mostCommonPurposeSubtitle,
                mostCommonAgeRange,
                mostCommonAgeRangeShare,
                mostCommonAgeRangeSubtitle
        );
    }

    private String normalize(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        if (trimmed.isEmpty() || trimmed.toLowerCase().startsWith("all")) {
            return null;
        }
        if (trimmed.toLowerCase().startsWith("other")) {
            return "Other";
        }
        return trimmed;
    }

    private String normalizeStatus(String statusInput) {
        FtjsStatus enumVal = parseStatusEnum(statusInput);
        return enumVal != null ? enumVal.name() : normalize(statusInput);
    }

    private FtjsStatus parseStatusEnum(String statusInput) {
        String norm = normalize(statusInput);
        if (norm == null) {
            return null;
        }
        String sanitized = norm.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        try {
            return FtjsStatus.valueOf(sanitized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
