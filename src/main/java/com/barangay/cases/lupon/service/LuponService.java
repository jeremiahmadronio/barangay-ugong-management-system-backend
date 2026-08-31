package com.barangay.cases.lupon.service;

import com.barangay.cases.lupon.dto.LuponPanelMemberResponse;
import com.barangay.cases.lupon.dto.LuponReferRalRequest;
import com.barangay.cases.lupon.dto.LuponReferralResponse;
import com.barangay.cases.lupon.dto.PangkatMember;
import com.barangay.cases.lupon.model.LuponPanelMember;
import com.barangay.cases.lupon.model.LuponReferral;
import com.barangay.cases.lupon.repository.LuponPanelMemberRepository;
import com.barangay.cases.lupon.repository.LuponReferralRepository;
import com.barangay.cases.lupon.repository.PanelAttendanceRepository;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.CaseSession;
import com.barangay.cases.shared.model.CaseTimeline;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.repository.CaseSessionRepository;
import com.barangay.cases.shared.repository.CaseTimelineRepository;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.SessionStatus;
import com.barangay.zshared.enumerated.TimelineEventType;
import com.barangay.zshared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LuponService {


    private final LuponPanelMemberRepository luponPanelMemberRepository;
    private final LuponReferralRepository  lupReferralRepository;
    private PanelAttendanceRepository  panelAttendanceRepository;
    private final CaseRepository caseRepository;
    private final DepartmentRepository departmentRepository;
    private final CaseSessionRepository caseSessionRepository;
    private final OfficerRepository officerRepository;
    private final CaseTimelineRepository caseTimelineRepository;
    private final com.barangay.cases.blotter.service.BlotterCaseService blotterCaseService;


    private Case findRootCase(Case targetCase) {
        Case root = targetCase;
        while (root.getPreviousCaseNumber() != null && !root.getPreviousCaseNumber().trim().isEmpty()) {
            Optional<Case> prevOpt = caseRepository.findByCaseNumber(root.getPreviousCaseNumber().trim());
            if (prevOpt.isPresent()) {
                root = prevOpt.get();
            } else {
                break;
            }
        }
        return root;
    }

    private List<Case> getCaseLineage(Case rootCase) {
        List<Case> lineage = new ArrayList<>();
        Case curr = rootCase;
        lineage.add(curr);
        while (curr.getReOpenedCaseNumber() != null && !curr.getReOpenedCaseNumber().trim().isEmpty()) {
            Optional<Case> nextOpt = caseRepository.findByCaseNumber(curr.getReOpenedCaseNumber().trim());
            if (nextOpt.isPresent()) {
                curr = nextOpt.get();
                lineage.add(curr);
            } else {
                break;
            }
        }
        return lineage;
    }

    @Transactional
    public LuponReferralResponse processLuponReferral(
            String caseNumber,
            LuponReferRalRequest request
    ) {
        LocalDateTime now = LocalDateTime.now();

        Case targetCase = caseRepository.findByCaseNumber(caseNumber)
                .orElseThrow(() -> new NoSuchElementException("Case not found: " + caseNumber));

        Case rootCase = findRootCase(targetCase);
        List<Case> lineage = getCaseLineage(rootCase);
        Case endmostCase = lineage.get(lineage.size() - 1);

        if (Boolean.TRUE.equals(endmostCase.getIsDeleted()) || Boolean.TRUE.equals(endmostCase.getIsPermanentDeleted())) {
            throw new IllegalStateException("Cannot refer a deleted case to Lupon.");
        }
        if (Boolean.TRUE.equals(endmostCase.getIsArchived())) {
            throw new IllegalStateException("Cannot refer an archived case to Lupon.");
        }
        if (endmostCase.getLuponReferral() != null) {
            throw new IllegalStateException("Case " + endmostCase.getCaseNumber() + " is already referred to Lupon.");
        }

        Department dept = departmentRepository.findByNameIgnoreCase("LUPON")
                .or(() -> departmentRepository.findByNameIgnoreCase("LUPONG_TAGAPAMAYAPA"))
                .orElseThrow(() -> new NoSuchElementException("Department 'LUPON' not found in database."));

        // Cancel scheduled mediation sessions across ALL cases in the lineage (root and all children)
        List<Long> lineageIds = lineage.stream().map(Case::getId).toList();
        List<CaseSession> activeSessions = caseSessionRepository.findSessionsByCaseIdInAndStatus(
                lineageIds,
                SessionStatus.SCHEDULED
        );

        if (!activeSessions.isEmpty()) {
            for (CaseSession session : activeSessions) {
                session.setStatus(SessionStatus.CANCELLED);
                session.setNotes("System: Automatically cancelled due to Lupon conciliation referral.");
            }
            caseSessionRepository.saveAll(activeSessions);
        }

        LuponReferral referral = new LuponReferral();
        referral.setCases(endmostCase);
        referral.setReferredAt(now);
        referral.setInitialDeadline(now.plusDays(15));
        referral.setExtensionCount(0);
        referral = lupReferralRepository.save(referral);

        User actor = SecurityUtils.getCurrentUser();

        for (Case c : lineage) {
            c.setDepartment(dept);
            if (c.getLuponReferral() == null) {
                c.setLuponReferral(referral);
            }
        }

        endmostCase.setDepartment(dept);
        endmostCase.setCaseStatus(CaseStatus.UNDER_CONCILIATION);
        endmostCase.setCaseStatusRemarks("Referred to Lupon Tagapamayapa.");
        endmostCase.setCaseUpdatedBy(actor);
        endmostCase.setLuponReferral(referral);
        caseRepository.saveAll(lineage);

            List<PangkatMember> memberRequests = request.memberList();
        Set<Long> uniqueOfficerIds = new HashSet<>();
        List<LuponPanelMember> panelMembers = new ArrayList<>();

        for (PangkatMember dto : memberRequests) {
            if (!uniqueOfficerIds.add(dto.officerId())) {
                throw new IllegalArgumentException("Duplicate officer ID found in panel: " + dto.officerId());
            }

            Officer officer = officerRepository.findById(dto.officerId())
                    .orElseThrow(() -> new NoSuchElementException("Officer not found with ID: " + dto.officerId()));

            LuponPanelMember member = new LuponPanelMember();
            member.setCases(endmostCase);
            member.setOfficer(officer);
            member.setPosition(dto.position().trim().toUpperCase());
            member.setAssignedAt(now);

            panelMembers.add(member);
        }

        CaseTimeline timeline = new CaseTimeline();
        timeline.setCases(targetCase);
        timeline.setEventType(TimelineEventType.ESCALATED);
        timeline.setDescription("Case " + targetCase.getCaseNumber() + "Refer to Lupon Tagapamayapa." );
        timeline.setTitle("Escalated Lupon Tagapamayapa.");
        timeline.setPerformedBy(actor);
        timeline.setEventDate(LocalDateTime.now());
        caseTimelineRepository.save(timeline);


        List<LuponPanelMember> savedMembers = luponPanelMemberRepository.saveAll(panelMembers);

        // 8. Map to Response DTO
        List<LuponPanelMemberResponse> memberResponses = savedMembers.stream()
                .map(m -> new LuponPanelMemberResponse(
                        m.getId(),
                        m.getOfficer().getId(),
                        formatOfficerFullName(m.getOfficer()),
                        m.getPosition(),
                        m.getAssignedAt()
                ))
                .toList();

        return new LuponReferralResponse(
                referral.getId(),
                endmostCase.getCaseNumber(),
                referral.getReferredAt(),
                referral.getInitialDeadline(),
                referral.getExtendedDeadline(),
                referral.getExtensionCount(),
                memberResponses
        );
    }

    /**
     * Calculates Lupon KPI summary statistics with month-to-date vs full previous calendar month trend.
     *
     * @return {@link LuponStatsResponse}
     */
    @Transactional(readOnly = true)
    public com.barangay.cases.lupon.dto.LuponStatsResponse getLuponStats() {
        java.time.LocalDateTime currentMonthStart = java.time.YearMonth.now().atDay(1).atStartOfDay();
        java.time.LocalDateTime currentMonthEnd = java.time.LocalDateTime.now();
        java.time.LocalDateTime prevMonthStart = java.time.YearMonth.now().minusMonths(1).atDay(1).atStartOfDay();
        java.time.LocalDateTime prevMonthEnd = currentMonthStart.minusNanos(1);

        // 1. Total Referred (all-time & trend)
        long totalReferred = caseRepository.countTotalLuponReferredCases();
        long currentMonthReferred = caseRepository.countTotalLuponReferredCasesBetween(currentMonthStart, currentMonthEnd);
        long prevMonthReferred = caseRepository.countTotalLuponReferredCasesBetween(prevMonthStart, prevMonthEnd);
        String totalReferredTrend = calculateMonthlyTrend(currentMonthReferred, prevMonthReferred);

        // 2. Active Conciliation
        long activeConciliation = caseRepository.countActiveLuponConciliationCases();

        // 3. Successfully Settled
        long successfullySettled = caseRepository.countSuccessfullySettledLuponCases();

        // 4. CFA Issued
        long cfaIssued = caseRepository.countCfaIssuedLuponCases();

        return new com.barangay.cases.lupon.dto.LuponStatsResponse(
                totalReferred,
                totalReferredTrend,
                activeConciliation,
                successfullySettled,
                cfaIssued
        );
    }

    /**
     * Retrieves assigned Pangkat Tagapagkasundo panel members for a case.
     *
     * @param caseId primary key of the case
     * @return list of assigned {@link LuponPanelMemberResponse}
     */
    @Transactional(readOnly = true)
    public List<com.barangay.cases.lupon.dto.LuponPanelMemberResponse> getPanelMembers(Long caseId) {
        List<LuponPanelMember> members = luponPanelMemberRepository.findByCasesId(caseId);
        return members.stream()
                .map(m -> new com.barangay.cases.lupon.dto.LuponPanelMemberResponse(
                        m.getId(),
                        m.getOfficer() != null ? m.getOfficer().getId() : null,
                        formatOfficerFullName(m.getOfficer()),
                        m.getPosition(),
                        m.getAssignedAt()
                ))
                .toList();
    }

    /**
     * Retrieves paginated and filtered Lupong Tagapamayapa cases.
     * Only displays cases that were referred/escalated to Lupon (excluding For-The-Record).
     *
     * @param filter the filtering criteria and pagination parameters
     * @return {@link com.barangay.zshared.mapper.PageResponse} of {@link com.barangay.cases.lupon.dto.LuponTableRowResponse}
     */
    @Transactional(readOnly = true)
    public com.barangay.zshared.mapper.PageResponse<com.barangay.cases.lupon.dto.LuponTableRowResponse> getLuponCases(
            com.barangay.cases.lupon.dto.LuponFilterRequest filter
    ) {
        int page = filter != null ? filter.getPageNumber() : 0;
        int size = filter != null ? filter.getPageSize() : 10;

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page,
                size,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );
        org.springframework.data.domain.Page<Case> casePage = caseRepository.findLuponCasesFiltered(filter, pageable);

        org.springframework.data.domain.Page<com.barangay.cases.lupon.dto.LuponTableRowResponse> dtoPage =
                casePage.map(this::toLuponTableRowResponse);

        return com.barangay.zshared.mapper.PageResponse.of(dtoPage);
    }

    private com.barangay.cases.lupon.dto.LuponTableRowResponse toLuponTableRowResponse(Case rootCase) {
        if (rootCase == null) return null;

        Case trueRoot = findRootCase(rootCase);
        List<Case> lineage = getCaseLineage(trueRoot);
        Case latestCase = lineage.get(lineage.size() - 1);
        int cycleCount = lineage.size();
        boolean isReopened = cycleCount > 1;

        String filingTypeLabel = "Formal Complaint";

        String compName = "—";
        Complainant comp = latestCase.getComplainant() != null ? latestCase.getComplainant() : trueRoot.getComplainant();
        if (comp != null && comp.getPerson() != null) {
            String f = comp.getPerson().getFirstName() != null ? comp.getPerson().getFirstName() : "";
            String l = comp.getPerson().getLastName() != null ? comp.getPerson().getLastName() : "";
            String m = comp.getPerson().getMiddleName() != null ? comp.getPerson().getMiddleName() : "";
            compName = (l + (l.isBlank() || f.isBlank() ? "" : ", ") + f + (m.isBlank() ? "" : " " + m.charAt(0) + ".")).trim();
        }

        String respName = "—";
        Respondent resp = latestCase.getRespondent() != null ? latestCase.getRespondent() : trueRoot.getRespondent();
        if (resp != null && resp.getPerson() != null) {
            String f = resp.getPerson().getFirstName() != null ? resp.getPerson().getFirstName() : "";
            String l = resp.getPerson().getLastName() != null ? resp.getPerson().getLastName() : "";
            String m = resp.getPerson().getMiddleName() != null ? resp.getPerson().getMiddleName() : "";
            respName = (l + (l.isBlank() || f.isBlank() ? "" : ", ") + f + (m.isBlank() ? "" : " " + m.charAt(0) + ".")).trim();
        }

        LocalDateTime dateFiled = trueRoot.getCreatedAt() != null ? trueRoot.getCreatedAt() : LocalDateTime.now();
        LocalDateTime dateReferred = null;
        if (latestCase.getLuponReferral() != null && latestCase.getLuponReferral().getReferredAt() != null) {
            dateReferred = latestCase.getLuponReferral().getReferredAt();
        } else if (trueRoot.getLuponReferral() != null && trueRoot.getLuponReferral().getReferredAt() != null) {
            dateReferred = trueRoot.getLuponReferral().getReferredAt();
        } else {
            dateReferred = latestCase.getUpdatedAt() != null ? latestCase.getUpdatedAt() : dateFiled;
        }

        return new com.barangay.cases.lupon.dto.LuponTableRowResponse(
                trueRoot.getId(),
                trueRoot.getCaseNumber(),
                latestCase.getId(),
                latestCase.getCaseNumber(),
                filingTypeLabel,
                cycleCount,
                isReopened,
                latestCase.getCaseStatus(),
                compName,
                respName,
                dateFiled,
                dateReferred
        );
    }

    private String calculateMonthlyTrend(long currentCount, long prevCount) {
        if (prevCount == 0) {
            return currentCount > 0 ? "+100% vs last month" : "0% vs last month";
        }
        double diff = (double) (currentCount - prevCount) / prevCount * 100.0;
        if (diff > 0) {
            return String.format("+%.1f%% vs last month", diff);
        } else if (diff < 0) {
            return String.format("%.1f%% vs last month", diff);
        } else {
            return "0% vs last month";
        }
    }

    private String formatOfficerFullName(Officer officer) {
        if (officer == null || officer.getPerson() == null) {
            return "N/A";
        }
        Person person = officer.getPerson();
        String first = person.getFirstName() != null ? person.getFirstName().trim() : "";
        String last = person.getLastName() != null ? person.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? "N/A" : full;
    }

    /**
     * Extends the Lupon conciliation period by 15 days (one-time allowable extension).
     *
     * @param caseId  the case ID
     * @param request the extension request containing reason
     * @return updated {@link com.barangay.cases.blotter.dto.BlotterCaseDetailResponse}
     */
    @Transactional
    public com.barangay.cases.blotter.dto.BlotterCaseDetailResponse extendLuponCase(
            Long caseId,
            com.barangay.cases.blotter.dto.ExtendCaseRequest request
    ) {
        Case targetCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new NoSuchElementException("Lupon case not found: " + caseId));

        if (Boolean.TRUE.equals(targetCase.getIsDeleted()) || Boolean.TRUE.equals(targetCase.getIsPermanentDeleted())) {
            throw new IllegalStateException("Cannot extend a deleted case.");
        }
        if (Boolean.TRUE.equals(targetCase.getIsArchived())) {
            throw new IllegalStateException("Cannot extend an archived case.");
        }
        if (targetCase.getCaseStatus() == CaseStatus.SETTLED || targetCase.getCaseStatus() == CaseStatus.DISMISSED) {
            throw new IllegalStateException("Cannot extend a case that is already " + targetCase.getCaseStatus() + ".");
        }
        if (Boolean.TRUE.equals(targetCase.getIsExtended()) || (targetCase.getExtensionCount() != null && targetCase.getExtensionCount() >= 1)) {
            throw new IllegalStateException("This Lupon case has already been granted a 15-day extension. Cases can only be extended once.");
        }

        LocalDateTime now = LocalDateTime.now();
        User currentUser = SecurityUtils.getCurrentUser();

        targetCase.setIsExtended(true);
        targetCase.setExtensionCount(1);
        targetCase.setExtendedAt(now);
        targetCase.setExtensionReason(request.reason());
        targetCase.setExtendedBy(currentUser);
        targetCase.setCaseUpdatedBy(currentUser);

        caseRepository.save(targetCase);

        // Add to Case Timeline
        CaseTimeline timeline = new CaseTimeline();
        timeline.setCases(targetCase);
        timeline.setEventType(TimelineEventType.CONCILIATION_EXTENDED);
        timeline.setTitle("Conciliation Period Extended (15 Days)");
        timeline.setDescription("15-day conciliation extension granted under Katarungang Pambarangay. Reason: " + request.reason());
        timeline.setPerformedBy(currentUser);
        timeline.setEventDate(now);
        caseTimelineRepository.save(timeline);

        return blotterCaseService.getBlotterCaseById(targetCase.getId());
    }
}
