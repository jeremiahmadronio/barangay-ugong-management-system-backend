package com.barangay.zshared.scheduler;

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

/**
 * Scheduled job service for Barangay Council for the Protection of Children (BCPC) cases.
 * Runs daily at midnight to monitor pending cases.
 *
 * If a BCPC case remains in PENDING status for 7 days without recorded action
 * (such as custody turnover, inter-agency referral, or intervention program),
 * its status is automatically updated to EXPIRED_UNACTIONED and an audit timeline event is logged.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BcpcScheduler {

    private final CaseRepository caseRepository;
    private final CaseTimelineService caseTimelineService;

    /**
     * Daily automated check running every midnight (12:00 AM).
     * Any BCPC case that has been in PENDING status for more than 7 days
     * is automatically transitioned to EXPIRED_UNACTIONED.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void autoExpireOverduePendingCases() {
        log.info("[BcpcScheduler] Checking for overdue PENDING BCPC cases (7-day inaction limit)...");

        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        List<CaseStatus> pendingStatuses = List.of(CaseStatus.PENDING);

        List<Case> overdueCases = caseRepository.findPendingBcpcCasesForExpirationCheck(pendingStatuses, cutoff);

        if (overdueCases.isEmpty()) {
            log.info("[BcpcScheduler] No overdue PENDING BCPC cases found.");
            return;
        }

        int expiredCount = 0;

        for (Case aCase : overdueCases) {
            LocalDateTime filingDate = aCase.getCreatedAt() != null ? aCase.getCreatedAt() : cutoff;

            log.info("[BcpcScheduler] Auto-expiring BCPC case [ID={}, CaseNo={}] - Filed on: {}, Cutoff: {}",
                    aCase.getId(), aCase.getCaseNumber(), filingDate, cutoff);

            aCase.setCaseStatus(CaseStatus.EXPIRED_UNACTIONED);
            aCase.setCaseStatusRemarks("Case automatically marked as EXPIRED_UNACTIONED because it remained in PENDING status for more than 7 days without custody turnover, referral, or intervention action (initial filing date: "
                    + filingDate.toLocalDate() + ").");
            caseRepository.save(aCase);

            try {
                caseTimelineService.logEvent(
                        aCase,
                        TimelineEventType.STATUS_CHANGED,
                        "Case Status Updated to EXPIRED_UNACTIONED",
                        "Automatically expired by system: Case remained in PENDING status for 7 days without action (filed on "
                                + filingDate.toLocalDate() + ").",
                        null
                );
            } catch (Exception e) {
                log.warn("[BcpcScheduler] Failed to log timeline event for auto-expired BCPC case [ID={}]: {}", aCase.getId(), e.getMessage());
            }

            expiredCount++;
        }

        log.info("[BcpcScheduler] Overdue BCPC cases check completed. Total cases transitioned to EXPIRED_UNACTIONED: {}", expiredCount);
    }
}

