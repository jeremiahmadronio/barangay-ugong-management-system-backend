package com.barangay.cases.lupon.service;

import com.barangay.cases.blotter.dto.SessionFollowUpResponse;
import com.barangay.cases.lupon.dto.ConciliationSessionResponse;
import com.barangay.cases.lupon.dto.ScheduleConciliationRequest;
import com.barangay.cases.lupon.model.LuponPanelMember;
import com.barangay.cases.lupon.model.PanelAttendance;
import com.barangay.cases.lupon.repository.LuponPanelMemberRepository;
import com.barangay.cases.lupon.repository.PanelAttendanceRepository;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.CaseSession;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.repository.CaseSessionRepository;
import com.barangay.cases.shared.repository.SessionFollowUpRepository;
import com.barangay.cases.shared.repository.SessionMinutesRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.SessionStatus;
import com.barangay.zshared.enumerated.SessionType;
import com.barangay.zshared.exception.ResourceNotFoundException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * BDD-style unit tests for {@link ConciliationService}.
 */
@ExtendWith(MockitoExtension.class)
class ConciliationServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private CaseSessionRepository caseSessionRepository;

    @Mock
    private SessionMinutesRepository sessionMinutesRepository;

    @Mock
    private SessionFollowUpRepository sessionFollowUpRepository;

    @Mock
    private LuponPanelMemberRepository luponPanelMemberRepository;

    @Mock
    private PanelAttendanceRepository panelAttendanceRepository;

    @Mock
    private CaseTimelineService caseTimelineService;

    @InjectMocks
    private ConciliationService conciliationService;

    @Nested
    @DisplayName("getConciliationSessions Tests")
    class GetConciliationSessionsTests {

        /**
         * Verifies retrieval and mapping of conciliation sessions for a valid case.
         */
        @Test
        @DisplayName("given case with conciliation session when getConciliationSessions then return mapped session list")
        void givenCaseWithSessions_whenGetConciliationSessions_thenReturnMappedSessionList() {
            // Given
            Long caseId = 1L;
            Case targetCase = new Case();
            targetCase.setId(caseId);

            CaseSession session = new CaseSession();
            session.setId(10L);
            session.setHearingType(SessionType.CONCILIATION);
            session.setStatus(SessionStatus.SCHEDULED);
            session.setScheduledStart(LocalDateTime.now().plusDays(2));
            session.setVenue("Lupon Office");

            given(caseRepository.findById(caseId)).willReturn(Optional.of(targetCase));
            given(caseSessionRepository.findByCasesIdOrderByScheduledStartAsc(caseId)).willReturn(List.of(session));
            given(luponPanelMemberRepository.findByCasesId(caseId)).willReturn(Collections.emptyList());
            given(panelAttendanceRepository.findBySessionIdIn(List.of(10L))).willReturn(Collections.emptyList());
            given(sessionMinutesRepository.findFirstBySessionIdOrderByCreatedAtDesc(10L)).willReturn(Optional.empty());
            given(sessionFollowUpRepository.findBySessionIdOrderByCreatedAtDesc(10L)).willReturn(Collections.emptyList());

            // When
            List<ConciliationSessionResponse> responses = conciliationService.getConciliationSessions(caseId);

            // Then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getId()).isEqualTo(10L);
            assertThat(responses.get(0).getHearingType()).isEqualTo("CONCILIATION");
            assertThat(responses.get(0).getSessionTitle()).contains("Conciliation Hearing");
        }

        /**
         * Verifies exception when case is not found.
         */
        @Test
        @DisplayName("given non-existent case when getConciliationSessions then throw ResourceNotFoundException")
        void givenNonExistentCase_whenGetConciliationSessions_thenThrowResourceNotFoundException() {
            // Given
            Long caseId = 999L;
            given(caseRepository.findById(caseId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> conciliationService.getConciliationSessions(caseId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cancelConciliation Tests")
    class CancelConciliationTests {

        /**
         * Verifies cancellation of a scheduled conciliation session.
         */
        @Test
        @DisplayName("given scheduled session when cancelConciliation then mark cancelled and save")
        void givenScheduledSession_whenCancelConciliation_thenMarkCancelledAndSave() {
            // Given
            Long sessionId = 100L;
            Case aCase = new Case();
            aCase.setId(1L);

            CaseSession session = new CaseSession();
            session.setId(sessionId);
            session.setCases(aCase);
            session.setStatus(SessionStatus.SCHEDULED);

            given(caseSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
            given(caseSessionRepository.save(session)).willReturn(session);
            given(caseRepository.findById(1L)).willReturn(Optional.of(aCase));
            given(caseSessionRepository.findByCasesIdOrderByScheduledStartAsc(1L)).willReturn(List.of(session));
            given(luponPanelMemberRepository.findByCasesId(1L)).willReturn(Collections.emptyList());
            given(panelAttendanceRepository.findBySessionIdIn(List.of(sessionId))).willReturn(Collections.emptyList());
            given(sessionMinutesRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId)).willReturn(Optional.empty());
            given(sessionFollowUpRepository.findBySessionIdOrderByCreatedAtDesc(sessionId)).willReturn(Collections.emptyList());

            // When
            ConciliationSessionResponse response = conciliationService.cancelConciliation(sessionId);

            // Then
            assertThat(session.getStatus()).isEqualTo(SessionStatus.CANCELLED);
            then(caseSessionRepository).should().save(session);
        }

        /**
         * Verifies exception when session is not found for cancellation.
         */
        @Test
        @DisplayName("given non-existent session when cancelConciliation then throw ResourceNotFoundException")
        void givenNonExistentSession_whenCancelConciliation_thenThrowResourceNotFoundException() {
            // Given
            Long sessionId = 999L;
            given(caseSessionRepository.findById(sessionId)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> conciliationService.cancelConciliation(sessionId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
