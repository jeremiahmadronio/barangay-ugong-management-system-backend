package com.barangay.zshared.scheduler;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.cases.vawc.model.BarangayProtectionOrder;
import com.barangay.cases.vawc.repository.BarangayProtectionOrderRepository;
import com.barangay.zshared.enumerated.BpoStatus;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.TimelineEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduled jobs for VAWC cases and Barangay Protection Orders (BPO):
 * Runs daily at midnight to check active BPOs exceeding the 15-day validity/monitoring period.
 * If the 15-day monitoring period has elapsed and the case is not yet settled or referred,
 * updates BPO status to EXPIRED and updates the VAWC Case status to EXPIRED_UNACTIONED / Unsettled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VawcScheduler {

    private final BarangayProtectionOrderRepository barangayProtectionOrderRepository;
    private final CaseRepository caseRepository;
    private final CaseTimelineService caseTimelineService;

    /**
     * Daily automated scheduler running every midnight (12:00 AM) to automatically expire
     * overdue 15-day BPO monitoring validity periods and transition unsettled VAWC cases.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void autoExpireOverdueBpoMonitoring() {
        log.info("[VawcScheduler] Checking active 15-day BPO monitoring validity periods...");

        List<BpoStatus> activeStatuses = List.of(BpoStatus.ISSUED, BpoStatus.SERVED);
        List<BarangayProtectionOrder> activeBpos = barangayProtectionOrderRepository.findActiveBposForExpirationCheck(activeStatuses);

        if (activeBpos.isEmpty()) {
            log.info("[VawcScheduler] No active BPOs found for expiration check.");
            return;
        }

        int expiredCount = 0;
        LocalDate today = LocalDate.now();

        for (BarangayProtectionOrder bpo : activeBpos) {
            LocalDate expiryDate = bpo.getExpiredAt();
            if (expiryDate == null && bpo.getActivatedAt() != null) {
                expiryDate = bpo.getActivatedAt().toLocalDate().plusDays(15);
            }

            if (expiryDate != null && today.isAfter(expiryDate)) {
                Case aCase = bpo.getCases();
                if (aCase == null) continue;

                // Check if case is already in a terminal/referred state
                CaseStatus currentStatus = aCase.getCaseStatus();
                if (currentStatus == CaseStatus.SETTLED ||
                    currentStatus == CaseStatus.DISMISSED ||
                    currentStatus == CaseStatus.WITHDRAWN ||
                    currentStatus == CaseStatus.ISSUED_REFERRAL ||
                    currentStatus == CaseStatus.DECLINED_REFERRAL) {
                    // Mark BPO as EXPIRED without altering closed case status
                    bpo.setStatus(BpoStatus.EXPIRED);
                    barangayProtectionOrderRepository.save(bpo);
                    continue;
                }

                log.info("[VawcScheduler] Auto-expiring 15-day BPO #{} (Case #{}) - Expiry date: {}",
                        bpo.getBpoControlNumber(), aCase.getCaseNumber(), expiryDate);

                bpo.setStatus(BpoStatus.EXPIRED);
                barangayProtectionOrderRepository.save(bpo);

                aCase.setCaseStatus(CaseStatus.EXPIRED_UNACTIONED);
                aCase.setCaseStatusRemarks("The 15-day Barangay Protection Order validity and monitoring period (until "
                        + expiryDate + ") has elapsed without case settlement. Case status marked as EXPIRED_UNACTIONED.");
                caseRepository.save(aCase);

                try {
                    caseTimelineService.logEvent(
                            aCase,
                            TimelineEventType.STATUS_CHANGED,
                            "15-Day BPO Monitoring Period Expired",
                            "The 15-day validity and monitoring period for Barangay Protection Order #"
                                    + bpo.getBpoControlNumber()
                                    + " has concluded without settlement. Case status transitioned to EXPIRED_UNACTIONED.",
                            null
                    );
                } catch (Exception e) {
                    log.warn("[VawcScheduler] Failed to log timeline event for BPO expiration [ID={}]: {}", bpo.getId(), e.getMessage());
                }

                expiredCount++;
            }
        }

        log.info("[VawcScheduler] 15-day BPO monitoring check completed. Processed {} expired order(s).", expiredCount);
    }
}
