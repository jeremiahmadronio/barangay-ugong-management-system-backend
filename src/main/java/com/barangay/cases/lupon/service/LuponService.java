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
import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
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

        endmostCase.setDepartment(dept);
        endmostCase.setCaseStatus(CaseStatus.UNDER_CONCILIATION);
        endmostCase.setCaseStatusRemarks("Referred to Lupon Tagapamayapa.");
        endmostCase.setCaseUpdatedBy(actor);
        endmostCase.setLuponReferral(referral);
        caseRepository.save(endmostCase);

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
}
