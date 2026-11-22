package com.barangay.cases.bcpc.service;

import com.barangay.cases.bcpc.dto.BcpcInterventionCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcInterventionDto;
import com.barangay.cases.bcpc.model.BcpcCaseDetails;
import com.barangay.cases.bcpc.model.BcpcIntervention;
import com.barangay.cases.bcpc.repository.BcpcCaseDetailsRepository;
import com.barangay.cases.bcpc.repository.BcpcInterventionRepository;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.zshared.enumerated.BcpcInterventionType;
import com.barangay.zshared.enumerated.CaseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * BDD Unit tests for {@link BcpcInterventionService}.
 * Tests intervention program creation, status validations, and case retrieval.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BcpcInterventionService BDD Tests")
class BcpcInterventionServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private BcpcCaseDetailsRepository bcpcCaseDetailsRepository;

    @Mock
    private BcpcInterventionRepository bcpcInterventionRepository;

    @Mock
    private CaseTimelineService caseTimelineService;

    @InjectMocks
    private BcpcInterventionService bcpcInterventionService;

    @Nested
    @DisplayName("Create Intervention Tests")
    class CreateInterventionTests {

        @Test
        @DisplayName("givenOngoingCase_whenCreateIntervention_thenSuccessfullyCreateIntervention")
        void givenOngoingCase_whenCreateIntervention_thenSuccessfullyCreateIntervention() {
            // Given
            Case aCase = new Case();
            aCase.setId(1L);
            aCase.setCaseStatus(CaseStatus.ONGOING);

            BcpcCaseDetails details = new BcpcCaseDetails();
            details.setId(10L);

            BcpcInterventionCreateRequest request = new BcpcInterventionCreateRequest();
            request.setInterventionType(BcpcInterventionType.INDIVIDUAL_COUNSELING);
            request.setProgramTitle("Child Counseling Program");
            request.setDescription("Weekly counseling");
            request.setStartDate(LocalDate.now());

            given(caseRepository.findById(1L)).willReturn(Optional.of(aCase));
            given(bcpcCaseDetailsRepository.findByCasesId(1L)).willReturn(Optional.of(details));
            given(bcpcInterventionRepository.save(any(BcpcIntervention.class))).willAnswer(invocation -> {
                BcpcIntervention saved = invocation.getArgument(0);
                saved.setId(100L);
                return saved;
            });

            // When
            BcpcInterventionDto result = bcpcInterventionService.createIntervention(1L, request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getProgramTitle()).isEqualTo("Child Counseling Program");
            then(bcpcInterventionRepository).should().save(any(BcpcIntervention.class));
        }

        @Test
        @DisplayName("givenClosedCase_whenCreateIntervention_thenThrowResponseStatusException")
        void givenClosedCase_whenCreateIntervention_thenThrowResponseStatusException() {
            // Given
            Case aCase = new Case();
            aCase.setId(2L);
            aCase.setCaseStatus(CaseStatus.CLOSED);

            BcpcInterventionCreateRequest request = new BcpcInterventionCreateRequest();

            given(caseRepository.findById(2L)).willReturn(Optional.of(aCase));

            // When & Then
            assertThatThrownBy(() -> bcpcInterventionService.createIntervention(2L, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Cannot initiate intervention for a resolved or closed case");
        }
    }

    @Nested
    @DisplayName("Get Interventions Tests")
    class GetInterventionsTests {

        @Test
        @DisplayName("givenValidCaseId_whenGetInterventionsByCaseId_thenReturnList")
        void givenValidCaseId_whenGetInterventionsByCaseId_thenReturnList() {
            // Given
            Case aCase = new Case();
            aCase.setId(1L);

            BcpcCaseDetails details = new BcpcCaseDetails();
            details.setId(10L);

            BcpcIntervention intervention = new BcpcIntervention();
            intervention.setId(100L);
            intervention.setInterventionType(BcpcInterventionType.INDIVIDUAL_COUNSELING);
            intervention.setProgramTitle("Counseling");
            intervention.setStatus("ONGOING");

            given(caseRepository.findById(1L)).willReturn(Optional.of(aCase));
            given(bcpcCaseDetailsRepository.findByCasesId(1L)).willReturn(Optional.of(details));
            given(bcpcInterventionRepository.findByBcpcCaseDetailsIdOrderByCreatedAtDesc(10L))
                    .willReturn(List.of(intervention));

            // When
            List<BcpcInterventionDto> result = bcpcInterventionService.getInterventionsByCaseId(1L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProgramTitle()).isEqualTo("Counseling");
            then(bcpcInterventionRepository).should().findByBcpcCaseDetailsIdOrderByCreatedAtDesc(10L);
        }
    }
}
