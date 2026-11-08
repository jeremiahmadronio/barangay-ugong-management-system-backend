package com.barangay.cases.bcpc.service;

import com.barangay.cases.bcpc.dto.BcpcCarCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCarCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCarCaseUpdateRequest;
import com.barangay.cases.bcpc.dto.BcpcCaseUniversalDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseUpdateRequest;
import com.barangay.cases.bcpc.dto.BcpcCustodyLogDto;
import com.barangay.cases.bcpc.dto.BcpcFilterRequest;
import com.barangay.cases.bcpc.dto.BcpcInterventionCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcInterventionDto;
import com.barangay.cases.bcpc.dto.BcpcInterventionProgressRequest;
import com.barangay.cases.bcpc.dto.BcpcInterventionUpdateRequest;
import com.barangay.cases.bcpc.dto.BcpcPersonInputDto;
import com.barangay.cases.bcpc.dto.BcpcReferralCancelRequest;
import com.barangay.cases.bcpc.dto.BcpcReferralCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcReferralFeedbackRequest;
import com.barangay.cases.bcpc.dto.BcpcReferralResponse;
import com.barangay.cases.bcpc.dto.BcpcStatsResponse;
import com.barangay.cases.bcpc.dto.BcpcTableRowResponse;
import com.barangay.cases.bcpc.dto.UpdateCiclCustodyLogDto;
import com.barangay.cases.bcpc.model.BcpcCaseDetails;
import com.barangay.cases.bcpc.repository.BcpcCaseDetailsRepository;
import com.barangay.cases.bcpc.repository.BcpcInterventionRepository;
import com.barangay.cases.bcpc.repository.BcpcReferralRepository;
import com.barangay.cases.blotter.dto.CaseNoteRequest;
import com.barangay.cases.blotter.dto.CaseNoteResponse;
import com.barangay.cases.shared.model.CaseNote;
import com.barangay.cases.shared.repository.CaseNoteRepository;
import com.barangay.cases.shared.model.Case;
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

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    // Specialized Domain Services
    private final BcpcCarCaseService bcpcCarCaseService;
    private final BcpcCiclCaseService bcpcCiclCaseService;
    private final BcpcCustodyService bcpcCustodyService;
    private final BcpcReferralService bcpcReferralService;
    private final BcpcInterventionService bcpcInterventionService;
    private final BcpcCommonService bcpcCommonService;

    // =========================================================================
    // CAR Case Delegation
    // =========================================================================

    public BcpcCarCaseDetailResponse createCarCase(BcpcCarCaseCreateRequest request) {
        return bcpcCarCaseService.createCarCase(request);
    }

    public BcpcCarCaseDetailResponse getCarCaseById(Long caseId) {
        return bcpcCarCaseService.getCarCaseById(caseId);
    }

    public BcpcCarCaseDetailResponse updateCarCase(Long caseId, BcpcCarCaseUpdateRequest request) {
        return bcpcCarCaseService.updateCarCase(caseId, request);
    }

    // =========================================================================
    // CICL Case Delegation
    // =========================================================================

    public BcpcCiclCaseDetailResponse createCiclCase(BcpcCiclCaseCreateRequest request) {
        return bcpcCiclCaseService.createCiclCase(request);
    }

    public BcpcCiclCaseDetailResponse getCiclCaseById(Long caseId) {
        return bcpcCiclCaseService.getCiclCaseById(caseId);
    }

    public BcpcCiclCaseDetailResponse updateCiclCase(Long caseId, BcpcCiclCaseUpdateRequest request) {
        return bcpcCiclCaseService.updateCiclCase(caseId, request);
    }

    // =========================================================================
    // Custody Delegation
    // =========================================================================

    public BcpcCustodyLogDto getCustodyLog(Long caseId) {
        return bcpcCustodyService.getCustodyLog(caseId);
    }

    public BcpcCustodyLogDto updateCustodyLog(Long caseId, BcpcCustodyLogDto request) {
        return bcpcCustodyService.updateCustodyLog(caseId, request);
    }

    public BcpcCustodyLogDto updateCiclCustodyLog(Long caseId, UpdateCiclCustodyLogDto request) {
        return bcpcCustodyService.updateCiclCustodyLog(caseId, request);
    }

    // =========================================================================
    // Referral Delegation
    // =========================================================================

    public List<BcpcReferralResponse> getReferrals(Long caseId) {
        return bcpcReferralService.getReferrals(caseId);
    }

    public BcpcReferralResponse issueReferral(Long caseId, BcpcReferralCreateRequest request) {
        return bcpcReferralService.issueReferral(caseId, request);
    }

    public BcpcReferralResponse recordReferralFeedback(Long caseId, Long referralId, BcpcReferralFeedbackRequest request) {
        return bcpcReferralService.recordReferralFeedback(caseId, referralId, request);
    }

    public BcpcReferralResponse cancelReferral(Long caseId, Long referralId, BcpcReferralCancelRequest request) {
        return bcpcReferralService.cancelReferral(caseId, referralId, request);
    }

    // =========================================================================
    // Intervention Delegation
    // =========================================================================

    public List<BcpcInterventionDto> getInterventionsByCaseId(Long caseId) {
        return bcpcInterventionService.getInterventionsByCaseId(caseId);
    }

    public BcpcInterventionDto createIntervention(Long caseId, BcpcInterventionCreateRequest request) {
        return bcpcInterventionService.createIntervention(caseId, request);
    }

    public BcpcInterventionDto updateIntervention(Long caseId, Long interventionId, BcpcInterventionUpdateRequest request) {
        return bcpcInterventionService.updateIntervention(caseId, interventionId, request);
    }

    public BcpcInterventionDto recordInterventionProgress(Long caseId, Long interventionId, BcpcInterventionProgressRequest request) {
        return bcpcInterventionService.recordInterventionProgress(caseId, interventionId, request);
    }

    // =========================================================================
    // Common Person Utility Delegation
    // =========================================================================

    public Person resolveOrCreatePerson(BcpcPersonInputDto dto) {
        return bcpcCommonService.resolveOrCreatePerson(dto);
    }

    // =========================================================================
    // Universal Details & Case Notes
    // =========================================================================

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

    // =========================================================================
    // Statistics & Case Records Table
    // =========================================================================

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

    @Transactional(readOnly = true)
    public PageResponse<BcpcTableRowResponse> getBcpcCasesTable(BcpcFilterRequest filter) {
        int page = filter != null ? filter.getPageNumber() : 0;
        int size = filter != null ? filter.getPageSize() : 5;

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Case> casePage = caseRepository.findBcpcCasesFiltered(filter, pageable);

        Page<BcpcTableRowResponse> dtoPage = casePage.map(this::toBcpcTableRowResponse);
        return PageResponse.of(dtoPage);
    }

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
