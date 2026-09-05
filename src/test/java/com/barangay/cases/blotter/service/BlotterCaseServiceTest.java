package com.barangay.cases.blotter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.barangay.cases.blotter.dto.BlotterCaseDetailResponse;
import com.barangay.cases.blotter.dto.ReopenCaseRequest;
import com.barangay.cases.blotter.dto.UpdateCaseStatusRequest;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseEvidenceRepository;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import com.barangay.zshared.enumerated.TimelineEventType;
import com.barangay.zshared.exception.BadRequestException;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.mapper.CaseEvidenceMapper;
import com.barangay.zshared.mapper.ComplainantMapper;
import com.barangay.zshared.mapper.IncidentDetailMapper;
import com.barangay.zshared.mapper.NarrativeStatementMapper;
import com.barangay.zshared.mapper.RespondentMapper;
import com.barangay.zshared.mapper.WitnessMapper;

@ExtendWith(MockitoExtension.class)
class BlotterCaseServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private com.barangay.people.person.repository.PersonRepository personRepository;

    @Mock
    private com.barangay.people.officer.repository.OfficerRepository officerRepository;

    @Mock
    private com.barangay.usermanagement.department.repository.DepartmentRepository departmentRepository;

    @Mock
    private CaseEvidenceRepository caseEvidenceRepository;

    @Mock
    private com.barangay.people.witness.repository.WitnessRepository witnessRepository;

    @Mock
    private CaseTimelineService caseTimelineService;

    @Mock
    private com.barangay.cases.shared.repository.CaseSessionRepository caseSessionRepository;

    @Mock
    private com.barangay.cases.shared.repository.CertificateToFileActionRepository certificateToFileActionRepository;

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

    @Test
    @DisplayName("Should update case status and save dismissal/status remarks when valid case ID and request are provided")
    void shouldUpdateCaseStatusAndSaveRemarks_whenValidStatusAndReasonProvided() {
        // Given
        Long caseId = 1L;
        Case existingCase = new Case();
        existingCase.setId(caseId);
        existingCase.setCaseNumber("BLTR-2026-0001");
        existingCase.setCaseStatus(CaseStatus.PENDING);

        UpdateCaseStatusRequest request = new UpdateCaseStatusRequest("DISMISSED", "Complainant withdrew the complaint.");

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));
        when(caseRepository.save(any(Case.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        BlotterCaseDetailResponse response = blotterCaseService.updateCaseStatus(caseId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(existingCase.getCaseStatus()).isEqualTo(CaseStatus.DISMISSED);
        assertThat(existingCase.getCaseStatusRemarks()).isEqualTo("Complainant withdrew the complaint.");

        verify(caseRepository).save(existingCase);
        verify(caseTimelineService).logEvent(
                eq(existingCase),
                eq(TimelineEventType.STATUS_CHANGED),
                eq("Case Dismissed"),
                eq("Case dismissed. Remarks: Complainant withdrew the complaint."),
                any()
        );
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating status for non-existent case ID")
    void shouldThrowResourceNotFoundException_whenCaseNotFound() {
        // Given
        Long invalidId = 999L;
        UpdateCaseStatusRequest request = new UpdateCaseStatusRequest("DISMISSED", "Reason");
        when(caseRepository.findById(invalidId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> blotterCaseService.updateCaseStatus(invalidId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Blotter case not found with id: 999");
    }

    @Test
    @DisplayName("Should successfully reopen closed/dismissed case and create new case cycle with bi-directional linking")
    void shouldReopenCase_whenOldCaseIsDismissed() {
        // Given
        Long oldCaseId = 10L;
        Case oldCase = new Case();
        oldCase.setId(oldCaseId);
        oldCase.setCaseNumber("BC-20260805-0001");
        oldCase.setCaseStatus(CaseStatus.DISMISSED);
        oldCase.setFilingType(FilingType.FORMAL_COMPLAINT);

        ReopenCaseRequest request = new ReopenCaseRequest("New evidence surfaced regarding boundary dispute.");

        when(caseRepository.findById(oldCaseId)).thenReturn(Optional.of(oldCase));
        when(caseRepository.existsByCaseNumber(any())).thenReturn(false);
        when(caseRepository.save(any(Case.class))).thenAnswer(invocation -> {
            Case c = invocation.getArgument(0);
            if (c.getId() == null) c.setId(11L);
            return c;
        });

        // When
        BlotterCaseDetailResponse response = blotterCaseService.reopenCase(oldCaseId, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(oldCase.getReOpenedCaseNumber()).isNotNull();
        verify(caseRepository, atLeastOnce()).save(any(Case.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when attempting to reopen case that is still PENDING")
    void shouldThrowBadRequestException_whenOldCaseIsPending() {
        // Given
        Long oldCaseId = 15L;
        Case oldCase = new Case();
        oldCase.setId(oldCaseId);
        oldCase.setCaseStatus(CaseStatus.PENDING);

        ReopenCaseRequest request = new ReopenCaseRequest("Reopen request");

        when(caseRepository.findById(oldCaseId)).thenReturn(Optional.of(oldCase));

        // When & Then
        assertThatThrownBy(() -> blotterCaseService.reopenCase(oldCaseId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot reopen case with status: PENDING");
    }

    @Test
    @DisplayName("Should throw BadRequestException when case has already been reopened")
    void shouldThrowBadRequestException_whenCaseAlreadyReopened() {
        // Given
        Long oldCaseId = 20L;
        Case oldCase = new Case();
        oldCase.setId(oldCaseId);
        oldCase.setCaseStatus(CaseStatus.DISMISSED);
        oldCase.setReOpenedCaseNumber("BC-20260810-0002");

        ReopenCaseRequest request = new ReopenCaseRequest("Duplicate reopen request");

        when(caseRepository.findById(oldCaseId)).thenReturn(Optional.of(oldCase));

        // When & Then
        assertThatThrownBy(() -> blotterCaseService.reopenCase(oldCaseId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Case has already been reopened as BC-20260810-0002");
    }
}
