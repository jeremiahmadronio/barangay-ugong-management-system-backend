package com.barangay.cases.lupon.service;

import com.barangay.cases.blotter.service.BlotterCaseService;
import com.barangay.cases.lupon.dto.LuponPanelMemberResponse;
import com.barangay.cases.lupon.dto.LuponReferRalRequest;
import com.barangay.cases.lupon.dto.LuponReferralResponse;
import com.barangay.cases.lupon.dto.LuponStatsResponse;
import com.barangay.cases.lupon.dto.PangkatMember;
import com.barangay.cases.lupon.model.LuponPanelMember;
import com.barangay.cases.lupon.model.LuponReferral;
import com.barangay.cases.lupon.repository.LuponPanelMemberRepository;
import com.barangay.cases.lupon.repository.LuponReferralRepository;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.repository.CaseSessionRepository;
import com.barangay.cases.shared.repository.CaseTimelineRepository;
import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.zshared.enumerated.CaseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * BDD-style unit tests for {@link LuponService}.
 */
@ExtendWith(MockitoExtension.class)
class LuponServiceTest {

    @Mock
    private LuponPanelMemberRepository luponPanelMemberRepository;

    @Mock
    private LuponReferralRepository lupReferralRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CaseSessionRepository caseSessionRepository;

    @Mock
    private OfficerRepository officerRepository;

    @Mock
    private CaseTimelineRepository caseTimelineRepository;

    @Mock
    private BlotterCaseService blotterCaseService;

    @InjectMocks
    private LuponService luponService;

    @Nested
    @DisplayName("getLuponStats Tests")
    class GetLuponStatsTests {

        /**
         * Verifies calculating Lupon dashboard stats and settlement rate.
         */
        @Test
        @DisplayName("given valid case counts when getLuponStats then return computed stats")
        void givenValidCaseCounts_whenGetLuponStats_thenReturnComputedStats() {
            // Given
            given(caseRepository.countTotalLuponReferredCases()).willReturn(20L);
            given(caseRepository.countTotalLuponReferredCasesBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .willReturn(5L);
            given(caseRepository.countActiveLuponConciliationCases()).willReturn(8L);
            given(caseRepository.countSuccessfullySettledLuponCases()).willReturn(10L);
            given(caseRepository.countCfaIssuedLuponCases()).willReturn(2L);

            // When
            LuponStatsResponse response = luponService.getLuponStats();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalReferred()).isEqualTo(20L);
            assertThat(response.activeConciliation()).isEqualTo(8L);
            assertThat(response.successfullySettled()).isEqualTo(10L);
            assertThat(response.cfaIssued()).isEqualTo(2L);
            assertThat(response.settlementRate()).isEqualTo(50.0);
        }
    }

    @Nested
    @DisplayName("getPanelMembers Tests")
    class GetPanelMembersTests {

        /**
         * Verifies retrieving assigned Pangkat panel members for a case.
         */
        @Test
        @DisplayName("given case id with panel members when getPanelMembers then return mapped member list")
        void givenCaseIdWithMembers_whenGetPanelMembers_thenReturnMappedList() {
            // Given
            Long caseId = 10L;

            Person p = new Person();
            p.setFirstName("Juan");
            p.setLastName("Dela Cruz");

            Officer o = new Officer();
            o.setId(100L);
            o.setPerson(p);

            LuponPanelMember member = new LuponPanelMember();
            member.setId(1L);
            member.setOfficer(o);
            member.setPosition("CHAIRMAN");
            member.setAssignedAt(LocalDateTime.now());

            given(luponPanelMemberRepository.findByCasesId(caseId)).willReturn(List.of(member));

            // When
            List<LuponPanelMemberResponse> result = luponService.getPanelMembers(caseId);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).position()).isEqualTo("CHAIRMAN");
            assertThat(result.get(0).officerId()).isEqualTo(100L);
            assertThat(result.get(0).officerName()).contains("Juan Dela Cruz");
        }
    }

    @Nested
    @DisplayName("processLuponReferral Tests")
    class ProcessLuponReferralTests {

        /**
         * Verifies exception when case does not exist.
         */
        @Test
        @DisplayName("given non-existent case number when processLuponReferral then throw NoSuchElementException")
        void givenNonExistentCaseNumber_whenProcessLuponReferral_thenThrowNoSuchElementException() {
            // Given
            String caseNumber = "NON-EXISTENT";
            given(caseRepository.findByCaseNumber(caseNumber)).willReturn(Optional.empty());

            LuponReferRalRequest request = new LuponReferRalRequest(Collections.emptyList());

            // When & Then
            assertThatThrownBy(() -> luponService.processLuponReferral(caseNumber, request))
                    .isInstanceOf(NoSuchElementException.class);
        }

        /**
         * Verifies exception when case is already referred to Lupon.
         */
        @Test
        @DisplayName("given case already referred when processLuponReferral then throw IllegalStateException")
        void givenAlreadyReferredCase_whenProcessLuponReferral_thenThrowIllegalStateException() {
            // Given
            String caseNumber = "LUP-2026-001";
            Case c = new Case();
            c.setId(1L);
            c.setCaseNumber(caseNumber);
            c.setLuponReferral(new LuponReferral());

            given(caseRepository.findByCaseNumber(caseNumber)).willReturn(Optional.of(c));

            LuponReferRalRequest request = new LuponReferRalRequest(Collections.emptyList());

            // When & Then
            assertThatThrownBy(() -> luponService.processLuponReferral(caseNumber, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already referred to Lupon");
        }
    }
}
