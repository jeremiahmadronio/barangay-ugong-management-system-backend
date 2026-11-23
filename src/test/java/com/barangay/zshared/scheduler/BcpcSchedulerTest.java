package com.barangay.zshared.scheduler;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.TimelineEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * BDD Unit tests for {@link BcpcScheduler}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BcpcScheduler BDD Tests")
class BcpcSchedulerTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseTimelineService caseTimelineService;

    @InjectMocks
    private BcpcScheduler scheduler;

    @Nested
    @DisplayName("Auto-expire Overdue Pending Cases Tests")
    class AutoExpirePendingCasesTests {

        @Test
        @DisplayName("givenNoOverduePendingCases_whenAutoExpireOverduePendingCases_thenNoStatusChange")
        void givenNoOverduePendingCases_whenAutoExpireOverduePendingCases_thenNoStatusChange() {
            // Given
            given(caseRepository.findPendingBcpcCasesForExpirationCheck(any(), any(LocalDateTime.class)))
                    .willReturn(List.of());

            // When
            scheduler.autoExpireOverduePendingCases();

            // Then
            then(caseRepository).should(never()).save(any());
            then(caseTimelineService).should(never()).logEvent(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("givenOverduePendingCases_whenAutoExpireOverduePendingCases_thenTransitionToExpiredUnactionedAndLogTimeline")
        void givenOverduePendingCases_whenAutoExpireOverduePendingCases_thenTransitionToExpiredUnactionedAndLogTimeline() {
            // Given
            Case overdueCase = new Case();
            overdueCase.setId(101L);
            overdueCase.setCaseNumber("BCPC-2026-0001");
            overdueCase.setCaseStatus(CaseStatus.PENDING);
            overdueCase.setCreatedAt(LocalDateTime.now().minusDays(10));

            given(caseRepository.findPendingBcpcCasesForExpirationCheck(any(), any(LocalDateTime.class)))
                    .willReturn(List.of(overdueCase));

            // When
            scheduler.autoExpireOverduePendingCases();

            // Then
            assertThat(overdueCase.getCaseStatus()).isEqualTo(CaseStatus.EXPIRED_UNACTIONED);
            assertThat(overdueCase.getCaseStatusRemarks()).contains("EXPIRED_UNACTIONED");

            then(caseRepository).should().save(overdueCase);
            then(caseTimelineService).should().logEvent(
                    eq(overdueCase),
                    eq(TimelineEventType.STATUS_CHANGED),
                    eq("Case Status Updated to EXPIRED_UNACTIONED"),
                    any(),
                    eq(null)
            );
        }
    }
}
