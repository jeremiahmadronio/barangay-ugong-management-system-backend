package com.barangay.zshared.scheduler;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.cases.vawc.model.BarangayProtectionOrder;
import com.barangay.cases.vawc.repository.BarangayProtectionOrderRepository;
import com.barangay.zshared.enumerated.BpoStatus;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.TimelineEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * BDD Unit tests for {@link VawcScheduler}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VawcScheduler BDD Tests")
class VawcSchedulerTest {

    @Mock
    private BarangayProtectionOrderRepository barangayProtectionOrderRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseTimelineService caseTimelineService;

    @InjectMocks
    private VawcScheduler scheduler;

    @Nested
    @DisplayName("Auto-expire Overdue BPO Monitoring Tests")
    class AutoExpireBpoMonitoringTests {

        @Test
        @DisplayName("givenNoActiveBpos_whenAutoExpireOverdueBpoMonitoring_thenDoNothing")
        void givenNoActiveBpos_whenAutoExpireOverdueBpoMonitoring_thenDoNothing() {
            // Given
            given(barangayProtectionOrderRepository.findActiveBposForExpirationCheck(any()))
                    .willReturn(List.of());

            // When
            scheduler.autoExpireOverdueBpoMonitoring();

            // Then
            then(barangayProtectionOrderRepository).should(never()).save(any());
            then(caseRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("givenExpiredBpoWithActiveCase_whenAutoExpireOverdueBpoMonitoring_thenExpireBpoAndCaseAndLogTimeline")
        void givenExpiredBpoWithActiveCase_whenAutoExpireOverdueBpoMonitoring_thenExpireBpoAndCaseAndLogTimeline() {
            // Given
            Case mockCase = new Case();
            mockCase.setId(201L);
            mockCase.setCaseNumber("VAWC-2026-0001");
            mockCase.setCaseStatus(CaseStatus.UNDER_MEDIATION);

            BarangayProtectionOrder bpo = new BarangayProtectionOrder();
            bpo.setId(1L);
            bpo.setBpoControlNumber("BPO-2026-0001");
            bpo.setStatus(BpoStatus.ISSUED);
            bpo.setExpiredAt(LocalDate.now().minusDays(1));
            bpo.setCases(mockCase);

            given(barangayProtectionOrderRepository.findActiveBposForExpirationCheck(any()))
                    .willReturn(List.of(bpo));

            // When
            scheduler.autoExpireOverdueBpoMonitoring();

            // Then
            assertThat(bpo.getStatus()).isEqualTo(BpoStatus.EXPIRED);
            assertThat(mockCase.getCaseStatus()).isEqualTo(CaseStatus.EXPIRED_UNACTIONED);
            then(barangayProtectionOrderRepository).should().save(bpo);
            then(caseRepository).should().save(mockCase);
            then(caseTimelineService).should().logEvent(
                    eq(mockCase),
                    eq(TimelineEventType.STATUS_CHANGED),
                    eq("15-Day BPO Monitoring Period Expired"),
                    any(),
                    eq(null)
            );
        }

        @Test
        @DisplayName("givenExpiredBpoWithSettledCase_whenAutoExpireOverdueBpoMonitoring_thenExpireBpoWithoutAlteringCaseStatus")
        void givenExpiredBpoWithSettledCase_whenAutoExpireOverdueBpoMonitoring_thenExpireBpoWithoutAlteringCaseStatus() {
            // Given
            Case settledCase = new Case();
            settledCase.setId(202L);
            settledCase.setCaseNumber("VAWC-2026-0002");
            settledCase.setCaseStatus(CaseStatus.SETTLED);

            BarangayProtectionOrder bpo = new BarangayProtectionOrder();
            bpo.setId(2L);
            bpo.setBpoControlNumber("BPO-2026-0002");
            bpo.setStatus(BpoStatus.ISSUED);
            bpo.setExpiredAt(LocalDate.now().minusDays(1));
            bpo.setCases(settledCase);

            given(barangayProtectionOrderRepository.findActiveBposForExpirationCheck(any()))
                    .willReturn(List.of(bpo));

            // When
            scheduler.autoExpireOverdueBpoMonitoring();

            // Then
            assertThat(bpo.getStatus()).isEqualTo(BpoStatus.EXPIRED);
            assertThat(settledCase.getCaseStatus()).isEqualTo(CaseStatus.SETTLED);
            then(barangayProtectionOrderRepository).should().save(bpo);
            then(caseRepository).should(never()).save(settledCase);
        }
    }
}
