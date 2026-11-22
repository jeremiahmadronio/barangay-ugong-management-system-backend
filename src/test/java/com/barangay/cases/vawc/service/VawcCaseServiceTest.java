package com.barangay.cases.vawc.service;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.IncidentDetail;
import com.barangay.cases.shared.repository.CaseEvidenceRepository;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.cases.vawc.dto.VawcCaseDetailResponse;
import com.barangay.cases.vawc.dto.VawcFilterRequest;
import com.barangay.cases.vawc.dto.VawcStatsResponse;
import com.barangay.cases.vawc.dto.VawcTableRowResponse;
import com.barangay.cases.vawc.repository.BarangayProtectionOrderRepository;
import com.barangay.cases.vawc.repository.ChildInvolvedRepository;
import com.barangay.cases.vawc.repository.ReferralFeedbackRepository;
import com.barangay.cases.vawc.repository.VawcReferralRepository;
import com.barangay.cases.vawc.repository.ViolenceTypeRepository;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.people.witness.repository.WitnessRepository;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.zshared.enumerated.BpoStatus;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.mapper.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * BDD-style unit tests for {@link VawcCaseService}.
 */
@ExtendWith(MockitoExtension.class)
class VawcCaseServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private BarangayProtectionOrderRepository barangayProtectionOrderRepository;

    @Mock
    private ChildInvolvedRepository childInvolvedRepository;

    @Mock
    private ViolenceTypeRepository violenceTypeRepository;

    @Mock
    private CaseEvidenceRepository caseEvidenceRepository;

    @Mock
    private OfficerRepository officerRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private WitnessRepository witnessRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CaseTimelineService caseTimelineService;

    @Mock
    private VawcReferralRepository vawcReferralRepository;

    @Mock
    private ReferralFeedbackRepository referralFeedbackRepository;

    @Mock
    private ComplainantMapper complainantMapper;

    @Mock
    private RespondentMapper respondentMapper;

    @Mock
    private IncidentDetailMapper incidentDetailMapper;

    @Mock
    private NarrativeStatementMapper narrativeStatementMapper;

    @Mock
    private CaseEvidenceMapper caseEvidenceMapper;

    @Mock
    private WitnessMapper witnessMapper;

    @InjectMocks
    private VawcCaseService vawcCaseService;

    @Nested
    @DisplayName("getVawcStats Tests")
    class GetVawcStatsTests {

        /**
         * Verifies KPI stats and monthly trend calculation when data is present.
         */
        @Test
        @DisplayName("given case metrics when getVawcStats then return calculated stats with trends")
        void givenCaseMetrics_whenGetVawcStats_thenReturnCalculatedStats() {
            // Given
            given(caseRepository.countRootVawcCases()).willReturn(15L);
            given(caseRepository.countRootVawcCasesByCreatedAtGreaterThanEqual(any(LocalDateTime.class))).willReturn(10L);
            given(caseRepository.countRootVawcCasesByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(5L);
            given(caseRepository.countVawcCasesByCaseStatusIn(eq(List.of(CaseStatus.ISSUED_REFERRAL)))).willReturn(4L);
            given(barangayProtectionOrderRepository.countByStatusIn(eq(List.of(BpoStatus.ISSUED, BpoStatus.SERVED)))).willReturn(8L);
            given(caseRepository.countVawcCasesByCaseStatusIn(eq(List.of(CaseStatus.WITHDRAWN)))).willReturn(2L);

            // When
            VawcStatsResponse response = vawcCaseService.getVawcStats();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalCases()).isEqualTo(15L);
            assertThat(response.totalCasesTrend()).isEqualTo("+100.0% vs last month");
            assertThat(response.totalReferredCases()).isEqualTo(4L);
            assertThat(response.totalActiveBpo()).isEqualTo(8L);
            assertThat(response.totalWithdrawnCases()).isEqualTo(2L);

            then(caseRepository).should().countRootVawcCases();
            then(barangayProtectionOrderRepository).should().countByStatusIn(anyList());
        }

        /**
         * Verifies trend percentage when previous month has zero cases.
         */
        @Test
        @DisplayName("given zero cases in previous month when getVawcStats then return plus 100 percent trend")
        void givenZeroPreviousMonthCases_whenGetVawcStats_thenReturnPlusOneHundredPercent() {
            // Given
            given(caseRepository.countRootVawcCases()).willReturn(3L);
            given(caseRepository.countRootVawcCasesByCreatedAtGreaterThanEqual(any(LocalDateTime.class))).willReturn(3L);
            given(caseRepository.countRootVawcCasesByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(0L);
            given(caseRepository.countVawcCasesByCaseStatusIn(eq(List.of(CaseStatus.ISSUED_REFERRAL)))).willReturn(0L);
            given(barangayProtectionOrderRepository.countByStatusIn(anyList())).willReturn(1L);
            given(caseRepository.countVawcCasesByCaseStatusIn(eq(List.of(CaseStatus.WITHDRAWN)))).willReturn(0L);

            // When
            VawcStatsResponse response = vawcCaseService.getVawcStats();

            // Then
            assertThat(response.totalCases()).isEqualTo(3L);
            assertThat(response.totalCasesTrend()).isEqualTo("+100.0% vs last month");
            assertThat(response.totalActiveBpo()).isEqualTo(1L);
        }

        /**
         * Verifies negative trend percentage when case volume decreases.
         */
        @Test
        @DisplayName("given fewer cases than previous month when getVawcStats then return negative trend")
        void givenFewerCasesThanPreviousMonth_whenGetVawcStats_thenReturnNegativeTrend() {
            // Given
            given(caseRepository.countRootVawcCases()).willReturn(10L);
            given(caseRepository.countRootVawcCasesByCreatedAtGreaterThanEqual(any(LocalDateTime.class))).willReturn(3L);
            given(caseRepository.countRootVawcCasesByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).willReturn(6L);
            given(caseRepository.countVawcCasesByCaseStatusIn(eq(List.of(CaseStatus.ISSUED_REFERRAL)))).willReturn(1L);
            given(barangayProtectionOrderRepository.countByStatusIn(anyList())).willReturn(2L);
            given(caseRepository.countVawcCasesByCaseStatusIn(eq(List.of(CaseStatus.WITHDRAWN)))).willReturn(1L);

            // When
            VawcStatsResponse response = vawcCaseService.getVawcStats();

            // Then
            assertThat(response.totalCases()).isEqualTo(10L);
            assertThat(response.totalCasesTrend()).isEqualTo("-50.0% vs last month");
        }
    }

    @Nested
    @DisplayName("getVawcCaseById Tests")
    class GetVawcCaseByIdTests {

        /**
         * Verifies case details retrieval when the case exists.
         */
        @Test
        @DisplayName("given existing case ID when getVawcCaseById then return mapped detail response")
        void givenExistingCaseId_whenGetVawcCaseById_thenReturnCaseDetail() {
            // Given
            Long caseId = 101L;
            Case existingCase = new Case();
            existingCase.setId(caseId);
            existingCase.setCaseNumber("2026-VAWC-001");
            existingCase.setCreatedAt(LocalDateTime.now());

            given(caseRepository.findById(caseId)).willReturn(Optional.of(existingCase));

            // When
            VawcCaseDetailResponse response = vawcCaseService.getVawcCaseById(caseId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(caseId);
            assertThat(response.caseNumber()).isEqualTo("2026-VAWC-001");
        }

        /**
         * Verifies ResourceNotFoundException is thrown when case ID does not exist.
         */
        @Test
        @DisplayName("given non-existent case ID when getVawcCaseById then throw ResourceNotFoundException")
        void givenNonExistentCaseId_whenGetVawcCaseById_thenThrowException() {
            // Given
            Long caseId = 999L;
            given(caseRepository.findById(caseId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> vawcCaseService.getVawcCaseById(caseId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("VAWC Case not found with ID: 999");
        }
    }

    @Nested
    @DisplayName("getVawcCases Table & Filtering Tests")
    class GetVawcCasesTableTests {

        /**
         * Verifies paginated and mapped VAWC case rows.
         */
        @Test
        @DisplayName("given filter criteria when getVawcCases then return mapped page response")
        void givenFilterCriteria_whenGetVawcCases_thenReturnPageResponse() {
            // Given
            VawcFilterRequest filter = new VawcFilterRequest(
                    "Madronio",
                    "PENDING",
                    "HIGH-IMMINENT",
                    0,
                    5
            );

            Case c1 = new Case();
            c1.setId(1001L);
            c1.setCaseNumber("2026-VAWC-001");
            c1.setCaseStatus(CaseStatus.PENDING);
            c1.setRiskLevel("HIGH-IMMINENT");
            c1.setCreatedAt(LocalDateTime.of(2026, 9, 5, 10, 30));

            Person compP = new Person();
            compP.setFirstName("Jeremiah");
            compP.setLastName("Madronio");
            compP.setMiddleName("Dragon");
            Complainant comp = new Complainant();
            comp.setPerson(compP);
            c1.setComplainant(comp);

            Person respP = new Person();
            respP.setFirstName("Adelaida");
            respP.setLastName("Perez");
            Respondent resp = new Respondent();
            resp.setPerson(respP);
            c1.setRespondent(resp);

            IncidentDetail inc = new IncidentDetail();
            inc.setNatureOfComplaint("Physical Abuse");
            c1.setIncidentDetail(inc);

            Page<Case> casePage = new PageImpl<>(List.of(c1), PageRequest.of(0, 5), 1L);

            given(caseRepository.findVawcCasesFiltered(eq(filter), any(Pageable.class)))
                    .willReturn(casePage);

            // When
            PageResponse<VawcTableRowResponse> response = vawcCaseService.getVawcCases(filter);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1L);
            assertThat(response.getPageSize()).isEqualTo(5);

            VawcTableRowResponse row = response.getContent().get(0);
            assertThat(row.id()).isEqualTo(1001L);
            assertThat(row.caseNumber()).isEqualTo("2026-VAWC-001");
            assertThat(row.complainant()).isEqualTo("Madronio, Jeremiah D.");
            assertThat(row.respondent()).isEqualTo("Perez, Adelaida");
            assertThat(row.natureOfComplaint()).isEqualTo("Physical Abuse");
            assertThat(row.status()).isEqualTo(CaseStatus.PENDING);
            assertThat(row.riskLevel()).isEqualTo("HIGH-IMMINENT");
            assertThat(row.dateFiled()).isEqualTo("09/05/2026");
        }
    }
}
