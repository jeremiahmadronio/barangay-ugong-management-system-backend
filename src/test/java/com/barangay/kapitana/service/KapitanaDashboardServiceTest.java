package com.barangay.kapitana.service;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.IncidentDetail;
import com.barangay.clearance.repository.IssuedCertificateRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.kapitana.dto.KapitanaDashboardKpiResponse;
import com.barangay.kapitana.dto.KapitanaRecentCaseItem;
import com.barangay.kapitana.repository.KapitanaDashboardRepository;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.person.model.Person;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * BDD Unit tests for {@link KapitanaDashboardService}.
 * Verifies cross-department KPI aggregation and recent unified case feeds.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KapitanaDashboardService BDD Tests")
class KapitanaDashboardServiceTest {

    @Mock
    private KapitanaDashboardRepository kapitanaDashboardRepository;

    @Mock
    private IssuedCertificateRepository issuedCertificateRepository;

    @Mock
    private FirstTimeJobSeekerRepository firstTimeJobSeekerRepository;

    @Mock
    private ResidentRepository residentRepository;

    @InjectMocks
    private KapitanaDashboardService kapitanaDashboardService;

    @Nested
    @DisplayName("Dashboard KPI Tests")
    class DashboardKpiTests {

        @Test
        @DisplayName("givenDepartmentCounters_whenGetDashboardKpi_thenCalculateExecutiveMetricsAccurately")
        void givenDepartmentCounters_whenGetDashboardKpi_thenCalculateExecutiveMetricsAccurately() {
            // Given
            given(kapitanaDashboardRepository.countActiveBlotterCases()).willReturn(5L);
            given(kapitanaDashboardRepository.countSettledBlotterCases()).willReturn(10L);
            given(kapitanaDashboardRepository.countTotalBlotterCases()).willReturn(15L);

            given(kapitanaDashboardRepository.countActiveLuponCases()).willReturn(2L);
            given(kapitanaDashboardRepository.countSettledLuponCases()).willReturn(8L);
            given(kapitanaDashboardRepository.countTotalLuponCases()).willReturn(10L);

            given(kapitanaDashboardRepository.countActiveVawcCases()).willReturn(1L);
            given(kapitanaDashboardRepository.countSettledVawcCases()).willReturn(3L);
            given(kapitanaDashboardRepository.countTotalVawcCases()).willReturn(4L);

            given(kapitanaDashboardRepository.countActiveBcpcCases()).willReturn(3L);
            given(kapitanaDashboardRepository.countSettledBcpcCases()).willReturn(7L);
            given(kapitanaDashboardRepository.countTotalBcpcCases()).willReturn(10L);

            given(issuedCertificateRepository.countActiveCertificates()).willReturn(50L);
            given(firstTimeJobSeekerRepository.countTotalIssued()).willReturn(20L);

            given(kapitanaDashboardRepository.countTotalActiveCases()).willReturn(11L);
            given(kapitanaDashboardRepository.countTotalSettledCases()).willReturn(28L);
            given(kapitanaDashboardRepository.countTotalNonDeletedCases()).willReturn(39L);

            given(residentRepository.countTotalResidents()).willReturn(1000L);
            given(residentRepository.countActiveHeadsOfFamily()).willReturn(250L);
            given(residentRepository.countActiveSeniorCitizens(any(LocalDate.class))).willReturn(120L);
            given(residentRepository.countActiveMinors(any(LocalDate.class))).willReturn(300L);
            given(residentRepository.countActiveByGenderPrefix("M")).willReturn(490L);
            given(residentRepository.countActiveByGenderPrefix("F")).willReturn(510L);

            // When
            KapitanaDashboardKpiResponse response = kapitanaDashboardService.getDashboardKpi();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalActiveCases()).isEqualTo(11L);
            assertThat(response.getTotalSettledCases()).isEqualTo(28L);
            assertThat(response.getTotalCasesAcrossDepartments()).isEqualTo(39L);
            assertThat(response.getTotalClearancesIssued()).isEqualTo(50L);
            assertThat(response.getTotalFtjsIssued()).isEqualTo(20L);

            assertThat(response.getDepartmentOverview().getBlotter().getActiveCases()).isEqualTo(5L);
            assertThat(response.getDepartmentOverview().getLupon().getActiveCases()).isEqualTo(2L);
            assertThat(response.getDepartmentOverview().getVawc().getActiveCases()).isEqualTo(1L);
            assertThat(response.getDepartmentOverview().getBcpc().getActiveCases()).isEqualTo(3L);

            assertThat(response.getDemographics().getTotalResidents()).isEqualTo(1000L);
            assertThat(response.getDemographics().getHeadsOfFamily()).isEqualTo(250L);
            assertThat(response.getDemographics().getSeniorCitizens()).isEqualTo(120L);
            assertThat(response.getDemographics().getMinorsCount()).isEqualTo(300L);

            then(kapitanaDashboardRepository).should().countActiveBlotterCases();
            then(residentRepository).should().countTotalResidents();
        }
    }

    @Nested
    @DisplayName("Recent Cases Feed Tests")
    class RecentCasesTests {

        @Test
        @DisplayName("givenCasesAcrossDepartments_whenGetRecentCases_thenReturnMappedUnifiedFeed")
        void givenCasesAcrossDepartments_whenGetRecentCases_thenReturnMappedUnifiedFeed() {
            // Given
            Case mockCase = new Case();
            mockCase.setId(100L);
            mockCase.setCaseNumber("BU26-BLOTTER-0001");
            mockCase.setCaseStatus(CaseStatus.PENDING);
            mockCase.setFilingType(FilingType.FORMAL_COMPLAINT);
            mockCase.setCreatedAt(LocalDateTime.now());

            Department dept = new Department();
            dept.setName("BLOTTER");
            mockCase.setDepartment(dept);

            IncidentDetail incident = new IncidentDetail();
            incident.setNatureOfComplaint("Disturbance of Peace");
            mockCase.setIncidentDetail(incident);

            Person compPerson = new Person();
            compPerson.setFirstName("Juan");
            compPerson.setLastName("Dela Cruz");
            Complainant complainant = new Complainant();
            complainant.setPerson(compPerson);
            mockCase.setComplainant(complainant);

            Person respPerson = new Person();
            respPerson.setFirstName("Pedro");
            respPerson.setLastName("Penduko");
            Respondent respondent = new Respondent();
            respondent.setPerson(respPerson);
            mockCase.setRespondent(respondent);

            given(kapitanaDashboardRepository.findRecentCasesAcrossDepartments(any(Pageable.class)))
                    .willReturn(List.of(mockCase));

            // When
            List<KapitanaRecentCaseItem> result = kapitanaDashboardService.getRecentCases(10);

            // Then
            assertThat(result).hasSize(1);
            KapitanaRecentCaseItem item = result.get(0);
            assertThat(item.getId()).isEqualTo(100L);
            assertThat(item.getCaseNumber()).isEqualTo("BU26-BLOTTER-0001");
            assertThat(item.getDepartment()).isEqualTo("BLOTTER");
            assertThat(item.getIncidentNature()).isEqualTo("Disturbance of Peace");
            assertThat(item.getComplainantName()).isEqualTo("Dela Cruz, Juan");
            assertThat(item.getRespondentName()).isEqualTo("Penduko, Pedro");
            then(kapitanaDashboardRepository).should().findRecentCasesAcrossDepartments(any(Pageable.class));
        }

        @Test
        @DisplayName("givenNoCases_whenGetRecentCases_thenReturnEmptyList")
        void givenNoCases_whenGetRecentCases_thenReturnEmptyList() {
            // Given
            given(kapitanaDashboardRepository.findRecentCasesAcrossDepartments(any(Pageable.class)))
                    .willReturn(List.of());

            // When
            List<KapitanaRecentCaseItem> result = kapitanaDashboardService.getRecentCases(5);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
