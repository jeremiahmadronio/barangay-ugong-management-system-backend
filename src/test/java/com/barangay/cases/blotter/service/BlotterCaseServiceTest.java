package com.barangay.cases.blotter.service;

import com.barangay.cases.blotter.dto.BlotterCaseCreateRequest;
import com.barangay.cases.blotter.dto.BlotterCaseDetailResponse;
import com.barangay.cases.blotter.dto.ReopenCaseRequest;
import com.barangay.cases.blotter.dto.UpdateCaseStatusRequest;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseEvidenceRepository;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.repository.CaseSessionRepository;
import com.barangay.cases.shared.repository.CertificateToFileActionRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.witness.repository.WitnessRepository;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import com.barangay.zshared.enumerated.TimelineEventType;
import com.barangay.zshared.exception.BadRequestException;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.mapper.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;

/**
 * Unit tests for {@link BlotterCaseService}.
 */
@ExtendWith(MockitoExtension.class)
class BlotterCaseServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private OfficerRepository officerRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private CaseEvidenceRepository caseEvidenceRepository;

    @Mock
    private WitnessRepository witnessRepository;

    @Mock
    private CaseTimelineService caseTimelineService;

    @Mock
    private CaseSessionRepository caseSessionRepository;

    @Mock
    private CertificateToFileActionRepository certificateToFileActionRepository;

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
    private BlotterCaseService blotterCaseService;

    @Nested
    @DisplayName("updateCaseStatus Method Tests")
    class UpdateCaseStatusTests {

        /**
         * Verifies updating case status to DISMISSED and saving status remarks with timeline audit logging.
         */
        @Test
        @DisplayName("shouldUpdateCaseStatusAndSaveRemarks_whenValidStatusAndReasonProvided")
        void shouldUpdateCaseStatusAndSaveRemarks_whenValidStatusAndReasonProvided() {
            // Given
            Long caseId = 1L;
            Case existingCase = new Case();
            existingCase.setId(caseId);
            existingCase.setCaseNumber("BLTR-2026-0001");
            existingCase.setCaseStatus(CaseStatus.PENDING);

            UpdateCaseStatusRequest request = new UpdateCaseStatusRequest("DISMISSED", "Complainant withdrew the complaint.");

            given(caseRepository.findById(caseId)).willReturn(Optional.of(existingCase));
            given(caseRepository.save(any(Case.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            BlotterCaseDetailResponse response = blotterCaseService.updateCaseStatus(caseId, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(existingCase.getCaseStatus()).isEqualTo(CaseStatus.DISMISSED);
            assertThat(existingCase.getCaseStatusRemarks()).isEqualTo("Complainant withdrew the complaint.");

            then(caseRepository).should().save(existingCase);
            then(caseTimelineService).should().logEvent(
                    eq(existingCase),
                    eq(TimelineEventType.STATUS_CHANGED),
                    eq("Case Dismissed"),
                    eq("Case dismissed. Remarks: Complainant withdrew the complaint."),
                    any()
            );
        }

        /**
         * Verifies ResourceNotFoundException is thrown when updating status for a non-existent case.
         */
        @Test
        @DisplayName("shouldThrowResourceNotFoundException_whenCaseNotFound")
        void shouldThrowResourceNotFoundException_whenCaseNotFound() {
            // Given
            Long invalidId = 999L;
            UpdateCaseStatusRequest request = new UpdateCaseStatusRequest("DISMISSED", "Reason");
            given(caseRepository.findById(invalidId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> blotterCaseService.updateCaseStatus(invalidId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Blotter case not found with id: 999");
        }
    }

    @Nested
    @DisplayName("reopenCase Method Tests")
    class ReopenCaseTests {

        /**
         * Verifies successfully reopening a dismissed case, linking new cycle to previous case number.
         */
        @Test
        @DisplayName("shouldReopenCase_whenOldCaseIsDismissed")
        void shouldReopenCase_whenOldCaseIsDismissed() {
            // Given
            Long oldCaseId = 10L;
            Case oldCase = new Case();
            oldCase.setId(oldCaseId);
            oldCase.setCaseNumber("BC-20260805-0001");
            oldCase.setCaseStatus(CaseStatus.DISMISSED);
            oldCase.setFilingType(FilingType.FORMAL_COMPLAINT);

            ReopenCaseRequest request = new ReopenCaseRequest("New evidence surfaced regarding boundary dispute.");

            given(caseRepository.findById(oldCaseId)).willReturn(Optional.of(oldCase));
            given(caseRepository.existsByCaseNumber(any())).willReturn(false);
            given(caseRepository.save(any(Case.class))).willAnswer(invocation -> {
                Case c = invocation.getArgument(0);
                if (c.getId() == null) c.setId(11L);
                return c;
            });

            // When
            BlotterCaseDetailResponse response = blotterCaseService.reopenCase(oldCaseId, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(oldCase.getReOpenedCaseNumber()).isNotNull();
            then(caseRepository).should(atLeastOnce()).save(any(Case.class));
        }

        /**
         * Verifies BadRequestException is thrown when trying to reopen an ongoing pending case.
         */
        @Test
        @DisplayName("shouldThrowBadRequestException_whenOldCaseIsPending")
        void shouldThrowBadRequestException_whenOldCaseIsPending() {
            // Given
            Long oldCaseId = 15L;
            Case oldCase = new Case();
            oldCase.setId(oldCaseId);
            oldCase.setCaseStatus(CaseStatus.PENDING);

            ReopenCaseRequest request = new ReopenCaseRequest("Reopen request");

            given(caseRepository.findById(oldCaseId)).willReturn(Optional.of(oldCase));

            // When & Then
            assertThatThrownBy(() -> blotterCaseService.reopenCase(oldCaseId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cannot reopen case with status: PENDING");
        }

        /**
         * Verifies BadRequestException is thrown when case has already been reopened.
         */
        @Test
        @DisplayName("shouldThrowBadRequestException_whenCaseAlreadyReopened")
        void shouldThrowBadRequestException_whenCaseAlreadyReopened() {
            // Given
            Long oldCaseId = 20L;
            Case oldCase = new Case();
            oldCase.setId(oldCaseId);
            oldCase.setCaseStatus(CaseStatus.DISMISSED);
            oldCase.setReOpenedCaseNumber("BC-20260810-0002");

            ReopenCaseRequest request = new ReopenCaseRequest("Duplicate reopen request");

            given(caseRepository.findById(oldCaseId)).willReturn(Optional.of(oldCase));

            // When & Then
            assertThatThrownBy(() -> blotterCaseService.reopenCase(oldCaseId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Case has already been reopened as BC-20260810-0002");
        }
    }

    @Nested
    @DisplayName("createBlotterCase Method Tests")
    class CreateBlotterCaseTests {

        /**
         * Verifies successful creation of a For The Record case without a respondent.
         */
        @Test
        @DisplayName("shouldCreateForTheRecordCaseSuccessfully_whenNoRespondentProvided")
        void shouldCreateForTheRecordCaseSuccessfully_whenNoRespondentProvided() {
            // Given
            BlotterCaseCreateRequest request =
                    new BlotterCaseCreateRequest(
                            FilingType.FOR_RECORD_ONLY,
                            "REC-2026-9999",
                            null,
                            null,
                            null,
                            "Officer On Duty",
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            true
                    );

            given(caseRepository.existsByCaseNumber("REC-2026-9999")).willReturn(false);
            given(departmentRepository.findByName("BLOTTER")).willReturn(Optional.empty());
            given(caseRepository.save(any(Case.class))).willAnswer(invocation -> {
                Case c = invocation.getArgument(0);
                c.setId(100L);
                return c;
            });

            // When
            BlotterCaseDetailResponse response = blotterCaseService.createBlotterCase(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.filingType()).isEqualTo(FilingType.FOR_RECORD_ONLY);
            assertThat(response.caseStatus()).isEqualTo(CaseStatus.RECORDED);
            assertThat(response.respondent()).isNull();
            then(caseRepository).should(atLeastOnce()).save(any(Case.class));
        }
    }
}
