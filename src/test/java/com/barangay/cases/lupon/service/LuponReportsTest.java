package com.barangay.cases.lupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.barangay.cases.blotter.dto.QuickMediationSummaryDto;
import com.barangay.cases.blotter.dto.RecentBlotterCaseResponse;
import com.barangay.cases.lupon.dto.LuponDashboardKpiResponse;
import com.barangay.cases.lupon.projection.LuponDashboardKpiProjection;
import com.barangay.cases.shared.model.Case;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.person.model.Person;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.repository.CaseSessionRepository;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import com.barangay.zshared.enumerated.SessionStatus;

@ExtendWith(MockitoExtension.class)
class LuponReportsTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseSessionRepository caseSessionRepository;

    @InjectMocks
    private LuponReports luponReports;

    @Test
    @DisplayName("getDashboardKpis should calculate correct KPI metrics and dates")
    void testGetDashboardKpis() {
        LuponDashboardKpiProjection projection = new LuponDashboardKpiProjection() {
            @Override
            public Long getTotalReferredThisWeek() {
                return 7L;
            }

            @Override
            public Long getTotalUnderConciliation() {
                return 4L;
            }

            @Override
            public Long getTotalCfaIssuedThisWeek() {
                return 2L;
            }
        };

        when(caseRepository.getLuponDashboardCaseKpis(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(projection);

        when(caseSessionRepository.countUpcomingLuponSessionsThisWeek(
                any(LocalDateTime.class), any(LocalDateTime.class), eq(SessionStatus.SCHEDULED)))
                .thenReturn(3L);

        LuponDashboardKpiResponse result = luponReports.getDashboardKpis();

        assertThat(result).isNotNull();
        assertThat(result.totalReferredThisWeek()).isEqualTo(7L);
        assertThat(result.totalUnderConciliation()).isEqualTo(4L);
        assertThat(result.upcomingHearingsThisWeek()).isEqualTo(3L);
        assertThat(result.totalCfaIssuedThisWeek()).isEqualTo(2L);
        assertThat(result.weekStartDate()).isNotNull();
        assertThat(result.weekEndDate()).isNotNull();
    }

    @Test
    @DisplayName("getRecentCasesToday should deduplicate case cycles and retrieve endmost status")
    void testGetRecentCasesTodayDeduplication() {
        Person compPerson = new Person();
        compPerson.setFirstName("Maria");
        compPerson.setLastName("Santos");
        Complainant complainant = new Complainant();
        complainant.setPerson(compPerson);

        Person respPerson = new Person();
        respPerson.setFirstName("Juan");
        respPerson.setLastName("Dela Cruz");
        Respondent respondent = new Respondent();
        respondent.setPerson(respPerson);

        // Root Case (CYCLE 1)
        Case rootCase = new Case();
        rootCase.setId(101L);
        rootCase.setCaseNumber("LUP-2026-001");
        rootCase.setFilingType(FilingType.FORMAL_COMPLAINT);
        rootCase.setCaseStatus(CaseStatus.UNDER_CONCILIATION);
        rootCase.setComplainant(complainant);
        rootCase.setRespondent(respondent);
        rootCase.setCreatedAt(LocalDateTime.now());
        rootCase.setReOpenedCaseNumber("LUP-2026-001-R1");

        // Reopened Child Case (CYCLE 2)
        Case childCase = new Case();
        childCase.setId(102L);
        childCase.setCaseNumber("LUP-2026-001-R1");
        childCase.setPreviousCaseNumber("LUP-2026-001");
        childCase.setCaseStatus(CaseStatus.SETTLED);
        childCase.setFilingType(FilingType.FORMAL_COMPLAINT);
        childCase.setComplainant(complainant);
        childCase.setRespondent(respondent);
        childCase.setCreatedAt(LocalDateTime.now());

        when(caseRepository.findRecentLuponCasesToday(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(childCase, rootCase));

        when(caseRepository.findByCaseNumber("LUP-2026-001")).thenReturn(Optional.of(rootCase));
        when(caseRepository.findByCaseNumber("LUP-2026-001-R1")).thenReturn(Optional.of(childCase));

        List<RecentBlotterCaseResponse> result = luponReports.getRecentCasesToday();

        assertThat(result).hasSize(1);
        RecentBlotterCaseResponse item = result.get(0);
        assertThat(item.id()).isEqualTo(101L);
        assertThat(item.caseNumber()).isEqualTo("LUP-2026-001");
        assertThat(item.complainantName()).isEqualTo("Maria Santos");
        assertThat(item.respondentName()).isEqualTo("Juan Dela Cruz");
        assertThat(item.status()).isEqualTo(CaseStatus.SETTLED);
    }

    @Test
    @DisplayName("getQuickTodayConciliations should return today's scheduled conciliations")
    void testGetQuickTodayConciliations() {
        QuickMediationSummaryDto summary = new QuickMediationSummaryDto(
                55L,
                "LUP-2026-005",
                "Pedro Cruz vs. Ana Reyes",
                (short) 1,
                LocalDateTime.now(),
                "Lupon Office"
        );

        when(caseSessionRepository.findQuickTodayLuponConciliations(
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(summary));

        List<QuickMediationSummaryDto> result = luponReports.getQuickTodayConciliations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).caseNumber()).isEqualTo("LUP-2026-005");
        assertThat(result.get(0).partiesSummary()).isEqualTo("Pedro Cruz vs. Ana Reyes");
        assertThat(result.get(0).venue()).isEqualTo("Lupon Office");
    }
}
