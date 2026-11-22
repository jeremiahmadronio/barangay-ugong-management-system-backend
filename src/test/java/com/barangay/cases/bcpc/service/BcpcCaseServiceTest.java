package com.barangay.cases.bcpc.service;

import com.barangay.cases.bcpc.dto.BcpcDashboardKpiResponse;
import com.barangay.cases.bcpc.dto.BcpcTableRowResponse;
import com.barangay.cases.bcpc.repository.BcpcCaseDetailsRepository;
import com.barangay.cases.bcpc.repository.BcpcInterventionRepository;
import com.barangay.cases.bcpc.repository.BcpcReferralRepository;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseNoteRepository;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.zshared.enumerated.CaseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * BDD Unit tests for {@link BcpcCaseService}.
 * Tests dashboard KPI aggregation and filtered case listings.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BcpcCaseService Coordinator Tests")
class BcpcCaseServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private BcpcCaseDetailsRepository bcpcCaseDetailsRepository;

    @Mock
    private BcpcReferralRepository bcpcReferralRepository;

    @Mock
    private BcpcInterventionRepository bcpcInterventionRepository;

    @Mock
    private CaseNoteRepository caseNoteRepository;

    @Mock
    private CaseTimelineService caseTimelineService;

    @Mock
    private BcpcCarCaseService bcpcCarCaseService;

    @Mock
    private BcpcCiclCaseService bcpcCiclCaseService;

    @Mock
    private BcpcCustodyService bcpcCustodyService;

    @Mock
    private BcpcReferralService bcpcReferralService;

    @Mock
    private BcpcInterventionService bcpcInterventionService;

    @Mock
    private BcpcCommonService bcpcCommonService;

    @InjectMocks
    private BcpcCaseService bcpcCaseService;

    @Nested
    @DisplayName("Dashboard KPI Tests")
    class DashboardKpiTests {

        @Test
        @DisplayName("givenActiveMetrics_whenGetDashboardKpis_thenCalculateAccurateCounts")
        void givenActiveMetrics_whenGetDashboardKpis_thenCalculateAccurateCounts() {
            // Given
            given(caseRepository.countRootBcpcCasesByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(10L, 5L);
            given(caseRepository.countLatestBcpcCasesByCaseStatusIn(eq(List.of(CaseStatus.PENDING))))
                    .willReturn(3L);
            given(caseRepository.countLatestBcpcCasesByCaseStatusIn(eq(List.of(CaseStatus.ONGOING, CaseStatus.REOPENED))))
                    .willReturn(7L);
            given(bcpcReferralRepository.countPendingAgencyFeedback())
                    .willReturn(2L);

            // When
            BcpcDashboardKpiResponse response = bcpcCaseService.getDashboardKpis();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalNewCasesThisWeek()).isEqualTo(10L);
            assertThat(response.totalNeedsAssessment()).isEqualTo(3L);
            assertThat(response.totalActiveInterventions()).isEqualTo(7L);
            assertThat(response.totalPendingAgencyFeedback()).isEqualTo(2L);
            then(caseRepository).should().countLatestBcpcCasesByCaseStatusIn(eq(List.of(CaseStatus.PENDING)));
        }
    }

    @Nested
    @DisplayName("Recent Cases Tests")
    class RecentCasesTests {

        @Test
        @DisplayName("givenLimit_whenGetRecentCases_thenReturnList")
        void givenLimit_whenGetRecentCases_thenReturnList() {
            // Given
            Case aCase = new Case();
            aCase.setId(10L);
            aCase.setCaseNumber("BU26-BCPC-0001");
            Page<Case> mockPage = new PageImpl<>(List.of(aCase));

            given(caseRepository.findBcpcCasesFiltered(any(), any(Pageable.class))).willReturn(mockPage);

            // When
            List<BcpcTableRowResponse> results = bcpcCaseService.getRecentCases(5);

            // Then
            assertThat(results).isNotNull();
            then(caseRepository).should().findBcpcCasesFiltered(any(), any(Pageable.class));
        }
    }
}
