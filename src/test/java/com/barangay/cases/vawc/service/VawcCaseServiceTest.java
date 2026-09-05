package com.barangay.cases.vawc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseEvidenceRepository;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.cases.vawc.dto.VawcCaseDetailResponse;
import com.barangay.cases.vawc.dto.VawcStatsResponse;
import com.barangay.cases.vawc.repository.BarangayProtectionOrderRepository;
import com.barangay.cases.vawc.repository.ChildInvolvedRepository;
import com.barangay.cases.vawc.repository.ViolenceTypeRepository;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.witness.repository.WitnessRepository;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.zshared.enumerated.BpoStatus;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.mapper.CaseEvidenceMapper;
import com.barangay.zshared.mapper.ComplainantMapper;
import com.barangay.zshared.mapper.IncidentDetailMapper;
import com.barangay.zshared.mapper.NarrativeStatementMapper;
import com.barangay.zshared.mapper.RespondentMapper;
import com.barangay.zshared.mapper.WitnessMapper;

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

        @Test
        @DisplayName("Should accurately calculate total cases, trends vs last month, referred, active BPO, and withdrawn cases")
        void shouldReturnAccurateStatsWithTrend_whenDataExists() {
            // Given: 15 total cases, 10 this month, 5 last month (+100.0% increase)
            when(caseRepository.countRootVawcCases()).thenReturn(15L);
            when(caseRepository.countRootVawcCasesByCreatedAtGreaterThanEqual(any(LocalDateTime.class))).thenReturn(10L);
            when(caseRepository.countRootVawcCasesByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(5L);

            // Referred cases: 4
            when(caseRepository.countVawcCasesByCaseStatusIn(eq(List.of(CaseStatus.ISSUED_REFERRAL)))).thenReturn(4L);

            // Active BPOs (ISSUED, SERVED): 8
            when(barangayProtectionOrderRepository.countByStatusIn(eq(List.of(BpoStatus.ISSUED, BpoStatus.SERVED)))).thenReturn(8L);

            // Withdrawn cases: 2
            when(caseRepository.countVawcCasesByCaseStatusIn(eq(List.of(CaseStatus.WITHDRAWN)))).thenReturn(2L);

            // When
            VawcStatsResponse response = vawcCaseService.getVawcStats();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.totalCases()).isEqualTo(15L);
            assertThat(response.totalCasesTrend()).isEqualTo("+100.0% vs last month");
            assertThat(response.totalReferredCases()).isEqualTo(4L);
            assertThat(response.totalActiveBpo()).isEqualTo(8L);
            assertThat(response.totalWithdrawnCases()).isEqualTo(2L);

            verify(caseRepository).countRootVawcCases();
            verify(barangayProtectionOrderRepository).countByStatusIn(anyList());
        }

        @Test
        @DisplayName("Should handle 0 previous month count gracefully with +100.0% when current > 0")
        void shouldHandleZeroPreviousMonth_whenCurrentHasCases() {
            when(caseRepository.countRootVawcCases()).thenReturn(3L);
            when(caseRepository.countRootVawcCasesByCreatedAtGreaterThanEqual(any(LocalDateTime.class))).thenReturn(3L);
            when(caseRepository.countRootVawcCasesByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
            when(caseRepository.countVawcCasesByCaseStatusIn(eq(List.of(CaseStatus.ISSUED_REFERRAL)))).thenReturn(0L);
            when(barangayProtectionOrderRepository.countByStatusIn(anyList())).thenReturn(1L);
            when(caseRepository.countVawcCasesByCaseStatusIn(eq(List.of(CaseStatus.WITHDRAWN)))).thenReturn(0L);

            VawcStatsResponse response = vawcCaseService.getVawcStats();

            assertThat(response.totalCases()).isEqualTo(3L);
            assertThat(response.totalCasesTrend()).isEqualTo("+100.0% vs last month");
            assertThat(response.totalActiveBpo()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should handle negative monthly trend percentage accurately")
        void shouldHandleNegativeMonthlyTrend_whenCasesDecreased() {
            when(caseRepository.countRootVawcCases()).thenReturn(10L);
            when(caseRepository.countRootVawcCasesByCreatedAtGreaterThanEqual(any(LocalDateTime.class))).thenReturn(3L);
            when(caseRepository.countRootVawcCasesByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(6L);
            when(caseRepository.countVawcCasesByCaseStatusIn(eq(List.of(CaseStatus.ISSUED_REFERRAL)))).thenReturn(1L);
            when(barangayProtectionOrderRepository.countByStatusIn(anyList())).thenReturn(2L);
            when(caseRepository.countVawcCasesByCaseStatusIn(eq(List.of(CaseStatus.WITHDRAWN)))).thenReturn(1L);

            VawcStatsResponse response = vawcCaseService.getVawcStats();

            assertThat(response.totalCases()).isEqualTo(10L);
            assertThat(response.totalCasesTrend()).isEqualTo("-50.0% vs last month");
        }
    }

    @Nested
    @DisplayName("getVawcCaseById Tests")
    class GetVawcCaseByIdTests {

        @Test
        @DisplayName("Should retrieve case details by ID when case exists")
        void shouldReturnCaseDetail_whenCaseExists() {
            Long caseId = 101L;
            Case existingCase = new Case();
            existingCase.setId(caseId);
            existingCase.setCaseNumber("2026-VAWC-001");
            existingCase.setCreatedAt(LocalDateTime.now());

            when(caseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));

            VawcCaseDetailResponse response = vawcCaseService.getVawcCaseById(caseId);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(caseId);
            assertThat(response.caseNumber()).isEqualTo("2026-VAWC-001");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when case ID does not exist")
        void shouldThrowResourceNotFoundException_whenCaseNotFound() {
            Long caseId = 999L;
            when(caseRepository.findById(caseId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> vawcCaseService.getVawcCaseById(caseId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("VAWC Case not found with ID: 999");
        }
    }

    @Nested
    @DisplayName("getVawcCases Table & Filtering Tests")
    class GetVawcCasesTableTests {

        @Test
        @DisplayName("Should retrieve paginated and mapped VAWC cases with 5 items per page")
        void shouldReturnPaginatedVawcCases_whenFilterApplied() {
            // Given
            com.barangay.cases.vawc.dto.VawcFilterRequest filter = new com.barangay.cases.vawc.dto.VawcFilterRequest(
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

            com.barangay.people.person.model.Person compP = new com.barangay.people.person.model.Person();
            compP.setFirstName("Jeremiah");
            compP.setLastName("Madronio");
            compP.setMiddleName("Dragon");
            com.barangay.people.complainant.model.Complainant comp = new com.barangay.people.complainant.model.Complainant();
            comp.setPerson(compP);
            c1.setComplainant(comp);

            com.barangay.people.person.model.Person respP = new com.barangay.people.person.model.Person();
            respP.setFirstName("Adelaida");
            respP.setLastName("Perez");
            com.barangay.people.respondent.model.Respondent resp = new com.barangay.people.respondent.model.Respondent();
            resp.setPerson(respP);
            c1.setRespondent(resp);

            com.barangay.cases.shared.model.IncidentDetail inc = new com.barangay.cases.shared.model.IncidentDetail();
            inc.setNatureOfComplaint("Physical Abuse");
            c1.setIncidentDetail(inc);

            org.springframework.data.domain.Page<Case> casePage = new org.springframework.data.domain.PageImpl<>(
                    List.of(c1),
                    org.springframework.data.domain.PageRequest.of(0, 5),
                    1L
            );

            when(caseRepository.findVawcCasesFiltered(eq(filter), any(org.springframework.data.domain.Pageable.class)))
                    .thenReturn(casePage);

            // When
            com.barangay.zshared.mapper.PageResponse<com.barangay.cases.vawc.dto.VawcTableRowResponse> response =
                    vawcCaseService.getVawcCases(filter);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getTotalElements()).isEqualTo(1L);
            assertThat(response.getPageSize()).isEqualTo(5);

            com.barangay.cases.vawc.dto.VawcTableRowResponse row = response.getContent().get(0);
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
