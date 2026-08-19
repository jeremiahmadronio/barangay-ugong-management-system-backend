package com.barangay.cases.blotter.scheduler;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.TimelineEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Scheduled task service that automatically checks blotter cases for expiration.
 * In accordance with Katarungang Pambarangay rules:
 * The 15-day mediation deadline is calculated based on the ROOT CASE's initial filing timestamp,
 * regardless of how many times a case has been reopened.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BlotterCaseScheduler {

    private final CaseRepository caseRepository;
    private final CaseTimelineService caseTimelineService;

    /**
     * Scheduled job that runs every midnight at 12:00 AM to check active non-terminal cases for expiration.
     * If the elapsed days from the ROOT filing date exceeds 15 days, the case status
     * is automatically updated to EXPIRED_UNACTIONED and a timeline audit event is logged.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void autoExpireOverdueCases() {
        log.info("Starting automated check for expired blotter cases (15-day root deadline rule)...");

        List<CaseStatus> activeStatuses = List.of(
                CaseStatus.PENDING,
                CaseStatus.UNDER_MEDIATION,
                CaseStatus.UNDER_CONCILIATION
        );

        List<Case> activeCases = caseRepository.findActiveCasesForExpirationCheck(activeStatuses);
        int expiredCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (Case aCase : activeCases) {
            LocalDateTime rootFilingDate = getRootCaseFilingDate(aCase);
            if (rootFilingDate == null) continue;

            LocalDateTime expirationDeadline = rootFilingDate.plusDays(15);

            if (now.isAfter(expirationDeadline)) {
                log.info("Auto-expiring case [ID={}, CaseNo={}] - Root filing date: {}, Expiration deadline: {}",
                        aCase.getId(), aCase.getCaseNumber(), rootFilingDate, expirationDeadline);

                aCase.setCaseStatus(CaseStatus.EXPIRED_UNACTIONED);
                aCase.setCaseStatusRemarks("Case automatically marked as EXPIRED_UNACTIONED because the 15-day mediation period from initial root filing date ("
                        + rootFilingDate.toLocalDate() + ") has elapsed.");
                caseRepository.save(aCase);

                try {
                    caseTimelineService.logEvent(
                            aCase,
                            TimelineEventType.STATUS_CHANGED,
                            "Case Status Updated to EXPIRED_UNACTIONED",
                            "Automatically expired by system: 15-day mediation deadline from initial root filing date (" + rootFilingDate.toLocalDate() + ") reached.",
                            null
                    );
                } catch (Exception e) {
                    log.warn("Failed to log timeline event for auto-expired case [ID={}]: {}", aCase.getId(), e.getMessage());
                }

                expiredCount++;
            }
        }

        log.info("Automated expired blotter cases check completed. Total cases updated to EXPIRED_UNACTIONED: {}", expiredCount);
    }

    /**
     * Traverses the previousCaseNumber chain backwards to find the original root case creation timestamp.
     */
    public LocalDateTime getRootCaseFilingDate(Case currentCase) {
        if (currentCase == null) return null;

        Case root = currentCase;
        while (root.getPreviousCaseNumber() != null && !root.getPreviousCaseNumber().trim().isEmpty()) {
            Optional<Case> prevOpt = caseRepository.findByCaseNumber(root.getPreviousCaseNumber().trim());
            if (prevOpt.isPresent()) {
                root = prevOpt.get();
            } else {
                break;
            }
        }
        return root.getCreatedAt() != null ? root.getCreatedAt() : currentCase.getCreatedAt();
    }
}
