package com.barangay.cases.bcpc.service;

import com.barangay.cases.bcpc.dto.BcpcCarCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCarCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCarCaseUpdateRequest;
import com.barangay.cases.bcpc.dto.BcpcCaseUniversalDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseUpdateRequest;
import com.barangay.cases.bcpc.dto.BcpcCustodyLogDto;
import com.barangay.cases.bcpc.dto.BcpcDashboardKpiResponse;
import com.barangay.cases.bcpc.dto.BcpcDashboardMonitoringItem;
import com.barangay.cases.bcpc.dto.BcpcFilterRequest;
import com.barangay.cases.bcpc.dto.BcpcInterventionCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcInterventionDto;
import com.barangay.cases.bcpc.dto.BcpcInterventionProgressRequest;
import com.barangay.cases.bcpc.dto.BcpcInterventionUpdateRequest;
import com.barangay.cases.bcpc.dto.BcpcPersonInputDto;
import com.barangay.cases.bcpc.dto.BcpcCategoryComparisonResponse;
import com.barangay.cases.bcpc.dto.BcpcReferralCancelRequest;
import com.barangay.cases.bcpc.dto.BcpcReferralCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcReferralFeedbackRequest;
import com.barangay.cases.bcpc.dto.BcpcReferralResponse;
import com.barangay.cases.bcpc.dto.BcpcReportKpiResponse;
import com.barangay.cases.bcpc.dto.BcpcStatsResponse;
import com.barangay.cases.bcpc.dto.BcpcTableRowResponse;
import com.barangay.cases.bcpc.dto.UpdateCiclCustodyLogDto;
import com.barangay.cases.bcpc.model.BcpcCaseDetails;
import com.barangay.cases.bcpc.repository.BcpcCaseDetailsRepository;
import com.barangay.cases.bcpc.repository.BcpcInterventionRepository;
import com.barangay.cases.bcpc.repository.BcpcReferralRepository;
import com.barangay.cases.blotter.dto.CaseNoteRequest;
import com.barangay.cases.blotter.dto.CaseNoteResponse;
import com.barangay.cases.blotter.dto.CaseStatusBreakdownResponse;
import com.barangay.cases.blotter.dto.CaseStatusItemDto;
import com.barangay.cases.blotter.dto.DailyCaseTrendResponse;
import com.barangay.cases.blotter.dto.MonthlyCaseTrendResponse;
import com.barangay.cases.blotter.dto.SettlementEfficiencyResponse;
import com.barangay.cases.shared.model.CaseNote;
import com.barangay.cases.shared.repository.CaseNoteRepository;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.projection.BcpcCategoryComparisonProjection;
import com.barangay.cases.shared.projection.BcpcReportKpiProjection;
import com.barangay.cases.shared.projection.CaseCountByDayProjection;
import com.barangay.cases.shared.projection.CaseCountByMonthProjection;
import com.barangay.cases.shared.projection.CaseStatusCountProjection;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.people.person.model.Person;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.enumerated.BcpcCategory;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.TimelineEventType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.mapper.PageResponse;
import com.barangay.zshared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * High-level coordinator and general facade for BCPC case operations.
 * Delegates specialized category operations to dedicated domain services:
 * - {@link BcpcCarCaseService} for Child at Risk (CAR) cases
 * - {@link BcpcCiclCaseService} for Child in Conflict with the Law (CICL) cases
 * - {@link BcpcCustodyService} for Custody & Turnover logs
 * - {@link BcpcReferralService} for Inter-agency Referrals
 * - {@link BcpcInterventionService} for Interventions & Monitoring
 * - {@link BcpcCommonService} for Shared Person and Entity Utilities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BcpcCaseService {

    private final CaseRepository caseRepository;
    private final BcpcCaseDetailsRepository bcpcCaseDetailsRepository;
    private final BcpcReferralRepository bcpcReferralRepository;
    private final BcpcInterventionRepository bcpcInterventionRepository;
    private final CaseNoteRepository caseNoteRepository;
    private final CaseTimelineService caseTimelineService;

    // Specialized Domain Services for Category Specific Details
    private final BcpcCarCaseService bcpcCarCaseService;
    private final BcpcCiclCaseService bcpcCiclCaseService;



    /**
     * Retrieves universal case detail including CAR or CICL specific data.
     *
     * @param caseId case identifier
     * @return universal case details
     */
    @Transactional(readOnly = true)
    public BcpcCaseUniversalDetailResponse getCaseDetailsById(Long caseId) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + caseId));

        BcpcCaseDetails details = bcpcCaseDetailsRepository.findByCases(aCase)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC details not found for case ID: " + caseId));

        String category = details.getCategory() != null ? details.getCategory().name() : "CAR";
        BcpcCarCaseDetailResponse car = null;
        BcpcCiclCaseDetailResponse cicl = null;

        if (details.getCategory() == BcpcCategory.CICL) {
            cicl = bcpcCiclCaseService.getCiclCaseById(caseId);
        } else {
            car = bcpcCarCaseService.getCarCaseById(caseId);
        }

        String effectiveCaseStatus = resolveEffectiveStatus(aCase, details);

        return new BcpcCaseUniversalDetailResponse(
                aCase.getId(),
                aCase.getCaseNumber(),
                category,
                aCase.getFilingType() != null ? aCase.getFilingType().name() : "FORMAL_COMPLAINT",
                effectiveCaseStatus,
                car,
                cicl
        );
    }

    /**
     * Adds an internal case note to a BCPC case.
     *
     * @param caseId  case identifier
     * @param request case note request payload
     * @return created case note response
     */
    @Transactional
    public CaseNoteResponse addCaseNote(Long caseId, CaseNoteRequest request) {
        log.info("Adding BCPC case note to Case ID: {}", caseId);
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC Case not found with ID: " + caseId));

        User currentUser = SecurityUtils.getCurrentUser();

        CaseNote note = new CaseNote();
        note.setCases(caseEntity);
        note.setNote(request.getNote());
        note.setCreatedBy(currentUser);

        CaseNote saved = caseNoteRepository.save(note);

        try {
            caseTimelineService.logEvent(
                    caseEntity,
                    TimelineEventType.NOTE_ADDED,
                    "BCPC Case Note Added",
                    request.getNote(),
                    currentUser
            );
        } catch (Exception e) {
            log.warn("Failed to log timeline event for BCPC case note [caseId={}]: {}", caseEntity.getId(), e.getMessage());
        }

        return toCaseNoteResponse(saved);
    }

    /**
     * Retrieves all internal case notes for a specific BCPC case in descending order.
     *
     * @param caseId case identifier
     * @return list of case notes
     */
    @Transactional(readOnly = true)
    public List<CaseNoteResponse> getCaseNotes(Long caseId) {
        log.info("Retrieving BCPC case notes for Case ID: {}", caseId);
        return caseNoteRepository.findByCasesIdOrderByCreatedAtDesc(caseId)
                .stream()
                .map(this::toCaseNoteResponse)
                .toList();
    }

    private CaseNoteResponse toCaseNoteResponse(CaseNote n) {
        String createdBy = null;
        if (n.getCreatedBy() != null) {
            User u = n.getCreatedBy();
            if (u.getPerson() != null) {
                String first = u.getPerson().getFirstName() != null ? u.getPerson().getFirstName() : "";
                String last  = u.getPerson().getLastName()  != null ? u.getPerson().getLastName()  : "";
                createdBy = (first + " " + last).trim();
            } else {
                createdBy = u.getUsername() != null ? u.getUsername() : u.getSystemEmail();
            }
        }
        return CaseNoteResponse.builder()
                .id(n.getId())
                .caseId(n.getCases() != null ? n.getCases().getId() : null)
                .note(n.getNote())
                .createdBy(createdBy)
                .createdAt(n.getCreatedAt())
                .build();
    }


    /**
     * Computes high-level statistics for BCPC cases including total cases, monthly trend, and active counts.
     *
     * @return BCPC stats response
     */
    @Transactional(readOnly = true)
    public BcpcStatsResponse getBcpcStats() {
        LocalDateTime currentMonthStart = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime prevMonthStart = YearMonth.now().minusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime prevMonthEnd = currentMonthStart.minusNanos(1);

        long totalCases = caseRepository.countRootBcpcCases();
        long currentTotal = caseRepository.countRootBcpcCasesByCreatedAtGreaterThanEqual(currentMonthStart);
        long prevTotal = caseRepository.countRootBcpcCasesByCreatedAtBetween(prevMonthStart, prevMonthEnd);
        String totalCasesTrend = calculateMonthlyTrend(currentTotal, prevTotal);

        List<CaseStatus> activeStatuses = List.of(
                CaseStatus.PENDING,
                CaseStatus.ONGOING,
                CaseStatus.REOPENED,
                CaseStatus.ISSUED_REFERRAL
        );
        long totalActiveCases = caseRepository.countLatestBcpcCasesByCaseStatusIn(activeStatuses);
        long totalReferredCases = bcpcReferralRepository.countDistinctReferredCases();
        long totalSettledCases = caseRepository.countLatestBcpcCasesByCaseStatusIn(List.of(CaseStatus.SETTLED));

        return new BcpcStatsResponse(
                totalCases,
                totalCasesTrend,
                totalActiveCases,
                totalReferredCases,
                totalSettledCases
        );
    }

    private String calculateMonthlyTrend(long currentCount, long prevCount) {
        if (prevCount == 0) {
            if (currentCount > 0) return "+100.0% vs last month";
            return "0.0% vs last month";
        }
        double percentChange = ((double) (currentCount - prevCount) / prevCount) * 100.0;
        return String.format("%+.1f%% vs last month", percentChange);
    }

    /**
     * Retrieves the 4 operational weekly KPIs for the BCPC Dashboard.
     */
    @Transactional(readOnly = true)
    public BcpcDashboardKpiResponse getDashboardKpis() {
        LocalDate today = LocalDate.now();
        LocalDate mondayDate = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate sundayDate = today.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));

        LocalDateTime startOfWeek = mondayDate.atStartOfDay();
        LocalDateTime endOfWeek = sundayDate.atTime(LocalTime.MAX);

        LocalDateTime prevStartOfWeek = mondayDate.minusWeeks(1).atStartOfDay();
        LocalDateTime prevEndOfWeek = sundayDate.minusWeeks(1).atTime(LocalTime.MAX);

        // Total New Cases This Week & Trend vs Previous Week
        long totalNewCasesThisWeek = caseRepository.countRootBcpcCasesByCreatedAtBetween(startOfWeek, endOfWeek);
        long prevTotalCases = caseRepository.countRootBcpcCasesByCreatedAtBetween(prevStartOfWeek, prevEndOfWeek);
        String newCasesTrend = calculateWeeklyTrend(totalNewCasesThisWeek, prevTotalCases);

        // Needs Assessment / Action: Active cases with status PENDING
        long totalNeedsAssessment = caseRepository.countLatestBcpcCasesByCaseStatusIn(List.of(CaseStatus.PENDING));

        // Active Intervention Monitoring: Cases under active diversion / intervention (ONGOING, REOPENED)
        long totalActiveInterventions = caseRepository.countLatestBcpcCasesByCaseStatusIn(
                List.of(CaseStatus.ONGOING, CaseStatus.REOPENED)
        );

        // Pending Agency Feedback: Referrals awaiting official feedback
        long totalPendingAgencyFeedback = bcpcReferralRepository.countPendingAgencyFeedback();

        return new BcpcDashboardKpiResponse(
                mondayDate,
                sundayDate,
                totalNewCasesThisWeek,
                newCasesTrend,
                totalNeedsAssessment,
                totalActiveInterventions,
                totalPendingAgencyFeedback
        );
    }

    private String calculateWeeklyTrend(long currentCount, long prevCount) {
        if (prevCount == 0) {
            if (currentCount > 0) return "+100.0% vs last week";
            return "0.0% vs last week";
        }
        double percentChange = ((double) (currentCount - prevCount) / prevCount) * 100.0;
        return String.format("%+.1f%% vs last week", percentChange);
    }

    /**
     * Retrieves the most recent BCPC cases up to the specified limit.
     *
     * @param limit maximum number of rows to retrieve
     * @return list of recent table row responses
     */
    @Transactional(readOnly = true)
    public List<BcpcTableRowResponse> getRecentCases(int limit) {
        int safeLimit = limit > 0 ? limit : 5;
        Pageable pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Case> casePage = caseRepository.findBcpcCasesFiltered(new BcpcFilterRequest(null, null, null, 0, safeLimit), pageable);
        return casePage.map(this::toBcpcTableRowResponse).getContent();
    }

    /**
     * Retrieves active monitoring items including pending referrals and ongoing interventions.
     *
     * @param limit maximum number of items to return
     * @return monitoring feed items
     */
    @Transactional(readOnly = true)
    public List<BcpcDashboardMonitoringItem> getMonitoringFeed(int limit) {
        int safeLimit = limit > 0 ? limit : 6;
        List<BcpcDashboardMonitoringItem> items = new ArrayList<>();

        // Pending Referrals
        try {
            List<com.barangay.cases.bcpc.model.BcpcReferral> pendingReferrals = bcpcReferralRepository.findPendingReferralsForMonitoring();
            for (var r : pendingReferrals) {
                if (r.getCases() == null) continue;
                String childName = "—";
                String category = "CAR";
                if (r.getCases().getBcpcCaseDetails() != null) {
                    if (r.getCases().getBcpcCaseDetails().getChild() != null) {
                        childName = maskChildName(r.getCases().getBcpcCaseDetails().getChild());
                    }
                    if (r.getCases().getBcpcCaseDetails().getCategory() != null) {
                        category = r.getCases().getBcpcCaseDetails().getCategory().name();
                    }
                }
                LocalDateTime refDate = r.getReferralDate() != null ? r.getReferralDate() : r.getCreatedAt();
                long days = refDate != null ? java.time.temporal.ChronoUnit.DAYS.between(refDate.toLocalDate(), LocalDate.now()) : 0L;

                items.add(new BcpcDashboardMonitoringItem(
                        r.getId(),
                        r.getCases().getId(),
                        r.getCases().getCaseNumber(),
                        childName,
                        category,
                        "REFERRAL",
                        r.getPurpose() != null && !r.getPurpose().isBlank() ? r.getPurpose() : "Agency Endorsement",
                        r.getReferredTo() != null ? r.getReferredTo() : "Partner Agency",
                        "Awaiting Feedback",
                        refDate,
                        days
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch pending referrals for monitoring feed: {}", e.getMessage());
        }

        // Active Interventions
        try {
            List<com.barangay.cases.bcpc.model.BcpcIntervention> activeInterventions = bcpcInterventionRepository.findActiveInterventionsForMonitoring();
            for (var i : activeInterventions) {
                if (i.getBcpcCaseDetails() == null || i.getBcpcCaseDetails().getCases() == null) continue;
                Case c = i.getBcpcCaseDetails().getCases();
                String childName = "—";
                String category = "CAR";
                if (i.getBcpcCaseDetails().getChild() != null) {
                    childName = maskChildName(i.getBcpcCaseDetails().getChild());
                }
                if (i.getBcpcCaseDetails().getCategory() != null) {
                    category = i.getBcpcCaseDetails().getCategory().name();
                }
                LocalDateTime created = i.getCreatedAt() != null ? i.getCreatedAt() : LocalDateTime.now();
                long days = java.time.temporal.ChronoUnit.DAYS.between(created.toLocalDate(), LocalDate.now());

                items.add(new BcpcDashboardMonitoringItem(
                        i.getId(),
                        c.getId(),
                        c.getCaseNumber(),
                        childName,
                        category,
                        "INTERVENTION",
                        i.getProgramTitle() != null && !i.getProgramTitle().isBlank() ? i.getProgramTitle() : "Diversion / Counseling",
                        i.getFacilitatorOrAgency() != null && !i.getFacilitatorOrAgency().isBlank() ? i.getFacilitatorOrAgency() : "Barangay BCPC Desk",
                        "Active Monitoring",
                        created,
                        days
                ));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch active interventions for monitoring feed: {}", e.getMessage());
        }

        // Sort descending by itemDate (most recent first) and cap to safeLimit
        items.sort(Comparator.comparing(BcpcDashboardMonitoringItem::itemDate, Comparator.nullsLast(Comparator.reverseOrder())));
        if (items.size() > safeLimit) {
            return items.subList(0, safeLimit);
        }
        return items;
    }

    /**
     * Computes BCPC Report KPI metrics (Total Cases, Settled Cases, Active Interventions, Agency Referrals)
     * within a specified date range.
     *
     * @param startDate start date filter
     * @param endDate   end date filter
     * @return {@link BcpcReportKpiResponse}
     */
    @Transactional(readOnly = true)
    public BcpcReportKpiResponse getReportKpi(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now().minusMonths(6);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<CaseStatus> activeStatuses = List.of(
                CaseStatus.PENDING,
                CaseStatus.ONGOING,
                CaseStatus.REOPENED
        );

        BcpcReportKpiProjection metrics = caseRepository.getBcpcReportKpiMetrics(
                startDateTime,
                endDateTime,
                CaseStatus.SETTLED,
                activeStatuses,
                CaseStatus.ISSUED_REFERRAL
        );

        long totalCases = (metrics != null && metrics.getTotalCases() != null) ? metrics.getTotalCases().longValue() : 0L;
        long settledCases = (metrics != null && metrics.getSettledCases() != null) ? metrics.getSettledCases().longValue() : 0L;
        long activeInterventions = (metrics != null && metrics.getActiveInterventions() != null) ? metrics.getActiveInterventions().longValue() : 0L;
        long agencyReferrals = (metrics != null && metrics.getAgencyReferrals() != null) ? metrics.getAgencyReferrals().longValue() : 0L;

        double settlementRate = (totalCases > 0 && settledCases > 0)
                ? Math.round(((double) settledCases / totalCases) * 1000.0) / 10.0
                : 0.0;

        return new BcpcReportKpiResponse(
                startDate,
                endDate,
                totalCases,
                settledCases,
                settlementRate,
                activeInterventions,
                agencyReferrals
        );
    }

    /**
     * Generates a monthly case filing trend report for BCPC with zero-filled counts.
     */
    @Transactional(readOnly = true)
    public List<MonthlyCaseTrendResponse> getMonthlyTrendReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusMonths(6);
        if (endDate == null) endDate = LocalDate.now();
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date is after end date");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<CaseCountByMonthProjection> dbResult = caseRepository.getBcpcMonthlyCaseCount(startDateTime, endDateTime);

        Map<YearMonth, Long> countMap = new HashMap<>();
        if (dbResult != null) {
            for (CaseCountByMonthProjection p : dbResult) {
                if (p.getYear() != null && p.getMonth() != null) {
                    countMap.put(YearMonth.of(p.getYear(), p.getMonth()), p.getCount() != null ? p.getCount() : 0L);
                }
            }
        }

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
     * Generates a daily case filing trend report for BCPC with zero-filled counts.
     */
    @Transactional(readOnly = true)
    public List<DailyCaseTrendResponse> getDailyTrendReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusMonths(6);
        if (endDate == null) endDate = LocalDate.now();
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate cannot be after endDate");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<CaseCountByDayProjection> dbResults = caseRepository.getBcpcDailyCaseCounts(startDateTime, endDateTime);

        Map<LocalDate, Long> countMap = new HashMap<>();
        if (dbResults != null) {
            for (CaseCountByDayProjection p : dbResults) {
                if (p.getDate() != null) {
                    countMap.put(p.getDate(), p.getCount() != null ? p.getCount() : 0L);
                }
            }
        }

        DateTimeFormatter dayLabelFormatter = DateTimeFormatter.ofPattern("MMM d");
        List<DailyCaseTrendResponse> responseList = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            long totalCases = countMap.getOrDefault(current, 0L);

            responseList.add(new DailyCaseTrendResponse(
                    current,
                    current.format(dayLabelFormatter),
                    current.getDayOfMonth(),
                    totalCases
            ));

            current = current.plusDays(1);
        }

        return responseList;
    }

    /**
     * Generates a comprehensive status breakdown report for BCPC cases within a date range.
     * Excludes soft-deleted, permanently deleted, and archived cases.
     */
    @Transactional(readOnly = true)
    public CaseStatusBreakdownResponse getStatusBreakdownReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusMonths(6);
        if (endDate == null) endDate = LocalDate.now();
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<CaseStatusCountProjection> dbResults = caseRepository.getBcpcCaseStatusCounts(startDateTime, endDateTime);

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
                double rawPercentage = totalCases > 0 ? ((double) count / totalCases) * 100.0 : 0.0;
                double percentage = BigDecimal.valueOf(rawPercentage)
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue();

                breakdownList.add(new CaseStatusItemDto(
                        status,
                        formatStatusLabel(status),
                        count,
                        percentage
                ));
            }
        }

        breakdownList.sort(Comparator.comparingLong(CaseStatusItemDto::count).reversed());

        return new CaseStatusBreakdownResponse(
                startDate,
                endDate,
                null,
                totalCases,
                breakdownList
        );
    }

    private String formatStatusLabel(CaseStatus status) {
        if (status == null) return "Pending";
        return switch (status) {
            case PENDING -> "Pending";
            case ONGOING -> "Ongoing";
            case SETTLED -> "Settled";
            case CLOSED -> "Closed";
            case DISMISSED -> "Dismissed";
            case WITHDRAWN -> "Withdrawn";
            case ISSUED_REFERRAL -> "Issued Referral";
            case DECLINED_REFERRAL -> "Declined Referral";
            case REOPENED -> "Reopened";
            default -> formatEnumLabel(status.name());
        };
    }

    private String formatEnumLabel(String name) {
        if (name == null || name.isBlank()) return "";
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0)))
                  .append(p.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Generates a category comparison report between CAR and CICL within a date range.
     * Excludes soft-deleted, permanently deleted, and archived cases.
     */
    @Transactional(readOnly = true)
    public BcpcCategoryComparisonResponse getCategoryComparisonReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusMonths(6);
        if (endDate == null) endDate = LocalDate.now();
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        BcpcCategoryComparisonProjection metrics = caseRepository.getBcpcCategoryComparisonMetrics(startDateTime, endDateTime);

        long carCases = (metrics != null && metrics.getCarCases() != null) ? metrics.getCarCases().longValue() : 0L;
        long ciclCases = (metrics != null && metrics.getCiclCases() != null) ? metrics.getCiclCases().longValue() : 0L;
        long totalCases = carCases + ciclCases;

        double carPct = totalCases > 0
                ? BigDecimal.valueOf((carCases * 100.0) / totalCases).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        double ciclPct = totalCases > 0
                ? BigDecimal.valueOf((ciclCases * 100.0) / totalCases).setScale(1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        return new BcpcCategoryComparisonResponse(
                startDate,
                endDate,
                totalCases,
                carCases,
                carPct,
                ciclCases,
                ciclPct
        );
    }

    /**
     * Generates a settlement efficiency performance report for BCPC cases within a date range.
     * Evaluates total recorded BCPC cases vs settled cases.
     */
    @Transactional(readOnly = true)
    public SettlementEfficiencyResponse getSettlementEfficiencyReport(LocalDate startDate, LocalDate endDate) {
        if (startDate == null) startDate = LocalDate.now().minusMonths(6);
        if (endDate == null) endDate = LocalDate.now();
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date.");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<CaseStatus> activeStatuses = List.of(
                CaseStatus.PENDING,
                CaseStatus.ONGOING,
                CaseStatus.REOPENED
        );

        BcpcReportKpiProjection metrics = caseRepository.getBcpcReportKpiMetrics(
                startDateTime,
                endDateTime,
                CaseStatus.SETTLED,
                activeStatuses,
                CaseStatus.ISSUED_REFERRAL
        );

        long totalCases = (metrics != null && metrics.getTotalCases() != null) ? metrics.getTotalCases().longValue() : 0L;
        long settledCases = (metrics != null && metrics.getSettledCases() != null) ? metrics.getSettledCases().longValue() : 0L;

        double settlementRate = 0.0;
        if (totalCases > 0 && settledCases > 0) {
            double rawRate = ((double) settledCases / totalCases) * 100.0;
            settlementRate = BigDecimal.valueOf(rawRate)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new SettlementEfficiencyResponse(
                startDate,
                endDate,
                null,
                totalCases,
                totalCases,
                settledCases,
                settlementRate
        );
    }

    /**
     * Retrieves paginated and filtered BCPC cases for the cases management table.
     *
     * @param filter filtering and pagination criteria
     * @return paginated table response
     */
    @Transactional(readOnly = true)
    public PageResponse<BcpcTableRowResponse> getBcpcCasesTable(BcpcFilterRequest filter) {
        int page = filter != null ? filter.getPageNumber() : 0;
        int size = filter != null ? filter.getPageSize() : 5;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Case> casePage = caseRepository.findBcpcCasesFiltered(filter, pageable);

        Page<BcpcTableRowResponse> dtoPage = casePage.map(this::toBcpcTableRowResponse);
        return PageResponse.of(dtoPage);
    }

    /**
     * Resolves the effective business display status for a BCPC case based on child profile and status.
     *
     * @param c       the case entity
     * @param details associated BCPC details
     * @return effective status string
     */
    public String resolveEffectiveStatus(Case c, BcpcCaseDetails details) {
        if (c == null) return "PENDING";
        CaseStatus raw = c.getCaseStatus();
        if (raw == CaseStatus.SETTLED) return "SETTLED";
        if (raw == CaseStatus.DISMISSED) return "DISMISSED";
        if (raw == CaseStatus.WITHDRAWN) return "WITHDRAWN";
        if (raw == CaseStatus.CLOSED) return "CLOSED";
        if (raw == CaseStatus.ONGOING) return "ONGOING";

        Long caseId = c.getId();

        try {
            if (bcpcReferralRepository.countByCasesId(caseId) > 0) {
                return "ONGOING";
            }
        } catch (Exception ignored) {}

        try {
            Long detailsId = details != null ? details.getId() : null;
            if (detailsId == null) {
                var dOpt = bcpcCaseDetailsRepository.findByCasesId(caseId);
                if (dOpt.isPresent()) detailsId = dOpt.get().getId();
            }
            if (detailsId != null && bcpcInterventionRepository.countByBcpcCaseDetailsId(detailsId) > 0) {
                return "ONGOING";
            }
        } catch (Exception ignored) {}

        try {
            if (details != null) {
                boolean hasCustody = (details.getTemporaryPlacement() != null && !details.getTemporaryPlacement().isBlank()) ||
                        (details.getCustodyResolution() != null && !details.getCustodyResolution().isBlank() && !"Pending Resolution".equalsIgnoreCase(details.getCustodyResolution())) ||
                        details.getTransferDate() != null ||
                        details.getTurnoverDate() != null;
                if (hasCustody) {
                    return "ONGOING";
                }
            }
        } catch (Exception ignored) {}

        return "PENDING";
    }

    /**
     * Maps a Case entity into a table row response for BCPC views.
     *
     * @param c the case entity
     * @return table row response
     */
    public BcpcTableRowResponse toBcpcTableRowResponse(Case c) {
        if (c == null) return null;

        String childName = "—";
        String category = "CAR";
        String caseNature = "—";

        if (c.getBcpcCaseDetails() != null) {
            if (c.getBcpcCaseDetails().getChild() != null) {
                childName = maskChildName(c.getBcpcCaseDetails().getChild());
            }
            if (c.getBcpcCaseDetails().getCategory() != null) {
                category = c.getBcpcCaseDetails().getCategory().name();
            }
        }

        if (c.getIncidentDetail() != null && c.getIncidentDetail().getNatureOfComplaint() != null && !c.getIncidentDetail().getNatureOfComplaint().isBlank()) {
            caseNature = c.getIncidentDetail().getNatureOfComplaint().trim();
            if (caseNature.toUpperCase().startsWith("CAR:")) {
                caseNature = caseNature.substring(4).trim();
            } else if (caseNature.toUpperCase().startsWith("CICL:")) {
                caseNature = caseNature.substring(5).trim();
            }
        }

        String dateFiled = c.getCreatedAt() != null
                ? c.getCreatedAt().format(DateTimeFormatter.ofPattern("M/d/yyyy"))
                : "—";

        String effectiveStatus = resolveEffectiveStatus(c, c.getBcpcCaseDetails());

        return new BcpcTableRowResponse(
                c.getId(),
                c.getCaseNumber(),
                childName,
                caseNature,
                category,
                effectiveStatus,
                dateFiled,
                c.getCreatedAt()
        );
    }

    private String maskChildName(Person p) {
        if (p == null) return "—";
        String first = p.getFirstName() != null ? p.getFirstName().trim() : "";
        String last = p.getLastName() != null ? p.getLastName().trim() : "";
        if (first.isEmpty() && last.isEmpty()) return "—";

        String maskedLast;
        if (last.length() <= 1) {
            maskedLast = last.toUpperCase();
        } else {
            maskedLast = last.substring(0, 1).toUpperCase() + "*".repeat(Math.max(1, last.length() - 1));
        }
        String firstInitial = first.isEmpty() ? "" : first.substring(0, 1).toUpperCase() + ".";
        return firstInitial.isEmpty() ? maskedLast : maskedLast + ", " + firstInitial;
    }

    /**
     * Calculates the 4 lifecycle KPIs exclusively for BCPC cases:
     * Total Archived, Archived this month, Total in Trash, and Deleted this month.
     *
     * @return {@link com.barangay.cases.blotter.dto.BlotterLifecycleKpiResponse}
     */
    @Transactional(readOnly = true)
    public com.barangay.cases.blotter.dto.BlotterLifecycleKpiResponse getBcpcLifecycleKpi() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(java.time.LocalTime.MAX);

        com.barangay.cases.shared.projection.BlotterLifecycleKpiProjection metrics = caseRepository.getBcpcLifecycleKpiMetrics(
                startOfMonth,
                endOfMonth
        );

        long totalArchived = (metrics != null && metrics.getTotalArchived() != null)
                ? metrics.getTotalArchived().longValue() : 0L;
        long totalArchivedThisMonth = (metrics != null && metrics.getTotalArchivedThisMonth() != null)
                ? metrics.getTotalArchivedThisMonth().longValue() : 0L;
        long totalRecentlyDeleted = (metrics != null && metrics.getTotalRecentlyDeleted() != null)
                ? metrics.getTotalRecentlyDeleted().longValue() : 0L;
        long totalRecentlyDeletedThisMonth = (metrics != null && metrics.getTotalRecentlyDeletedThisMonth() != null)
                ? metrics.getTotalRecentlyDeletedThisMonth().longValue() : 0L;

        return new com.barangay.cases.blotter.dto.BlotterLifecycleKpiResponse(
                totalArchived,
                totalArchivedThisMonth,
                totalRecentlyDeleted,
                totalRecentlyDeletedThisMonth
        );
    }

    /**
     * Updates status of a BCPC case (e.g. SETTLED, DISMISSED, WITHDRAWN) with optional terms or closing remarks.
     *
     * @param caseId  the case ID
     * @param request status and remarks
     * @return updated universal case details
     */
    @Transactional
    public BcpcCaseUniversalDetailResponse updateCaseStatus(Long caseId, com.barangay.cases.blotter.dto.UpdateCaseStatusRequest request) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("BCPC case not found with id: " + caseId));

        if (Boolean.TRUE.equals(aCase.getIsDeleted()) || Boolean.TRUE.equals(aCase.getIsPermanentDeleted())) {
            throw new IllegalStateException("Cannot update status of a deleted case.");
        }

        if (request == null || request.getStatus() == null || request.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("Status is required");
        }

        CaseStatus newStatus;
        try {
            newStatus = CaseStatus.valueOf(request.getStatus().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid CaseStatus provided for BCPC: {}", request.getStatus());
            throw new IllegalArgumentException("Invalid case status: " + request.getStatus());
        }

        aCase.setCaseStatus(newStatus);
        String remarks = (request.getRemarks() != null && !request.getRemarks().isBlank())
                ? request.getRemarks().trim()
                : null;

        if (newStatus == CaseStatus.SETTLED) {
            aCase.setSettledAt(LocalDateTime.now());
            if (remarks != null) {
                aCase.setSettlementTerms(remarks);
            }
            caseTimelineService.logEvent(
                    aCase,
                    TimelineEventType.STATUS_CHANGED,
                    "BCPC Case Settled",
                    "Case marked as Settled. Details: " + (remarks != null ? remarks : "Completed diversion/intervention or amicable resolution."),
                    SecurityUtils.getCurrentUser()
            );
        } else if (newStatus == CaseStatus.CLOSED) {
            if (remarks != null) {
                aCase.setSettlementTerms(remarks);
            }
            caseTimelineService.logEvent(
                    aCase,
                    TimelineEventType.STATUS_CHANGED,
                    "BCPC Case Closed",
                    "Case closed. Reason: " + (remarks != null ? remarks : "Case officially closed."),
                    SecurityUtils.getCurrentUser()
            );
        } else if (newStatus == CaseStatus.DISMISSED || newStatus == CaseStatus.WITHDRAWN) {
            if (remarks != null) {
                aCase.setSettlementTerms(remarks);
            }
            caseTimelineService.logEvent(
                    aCase,
                    TimelineEventType.STATUS_CHANGED,
                    "BCPC Case " + (newStatus == CaseStatus.DISMISSED ? "Dismissed" : "Withdrawn"),
                    "Case " + (newStatus == CaseStatus.DISMISSED ? "dismissed" : "withdrawn") + ". Reason: " + (remarks != null ? remarks : (newStatus == CaseStatus.DISMISSED ? "Dismissed due to lack of merit or grounds." : "Withdrawn by parties.")),
                    SecurityUtils.getCurrentUser()
            );
        } else {
            caseTimelineService.logEvent(
                    aCase,
                    TimelineEventType.STATUS_CHANGED,
                    "BCPC Case Status Updated",
                    "Status updated to " + newStatus + (remarks != null ? ". Reason: " + remarks : ""),
                    SecurityUtils.getCurrentUser()
            );
        }

        caseRepository.save(aCase);
        log.info("BCPC Case #{} status successfully updated to {}", aCase.getCaseNumber(), newStatus);
        return getCaseDetailsById(caseId);
    }
}
