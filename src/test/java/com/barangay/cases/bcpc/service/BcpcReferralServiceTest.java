package com.barangay.cases.bcpc.service;

import com.barangay.cases.bcpc.dto.BcpcReferralCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcReferralResponse;
import com.barangay.cases.bcpc.model.BcpcReferral;
import com.barangay.cases.bcpc.repository.BcpcReferralFeedbackRepository;
import com.barangay.cases.bcpc.repository.BcpcReferralRepository;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.zshared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * BDD Unit tests for {@link BcpcReferralService}.
 * Verifies referral creation, validation, and retrieval flows.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BcpcReferralService BDD Tests")
class BcpcReferralServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private BcpcReferralRepository bcpcReferralRepository;

    @Mock
    private BcpcReferralFeedbackRepository bcpcReferralFeedbackRepository;

    @Mock
    private CaseTimelineService caseTimelineService;

    @InjectMocks
    private BcpcReferralService bcpcReferralService;

    @Nested
    @DisplayName("Issue Referral Tests")
    class IssueReferralTests {

        @Test
        @DisplayName("givenValidRequest_whenIssueReferral_thenSuccessfullyIssueReferral")
        void givenValidRequest_whenIssueReferral_thenSuccessfullyIssueReferral() {
            // Given
            Case aCase = new Case();
            aCase.setId(1L);
            aCase.setCaseNumber("BU26-BCPC-0001");

            BcpcReferralCreateRequest request = new BcpcReferralCreateRequest();
            request.setReferredTo("LSWDO");
            request.setPurpose("Social Evaluation");
            request.setGrounds("Minor in need of assessment");
            request.setReferralDate(LocalDateTime.now());
            request.setReferredBy("Officer Juan");

            given(caseRepository.findById(1L)).willReturn(Optional.of(aCase));
            given(bcpcReferralRepository.save(any(BcpcReferral.class))).willAnswer(invocation -> {
                BcpcReferral r = invocation.getArgument(0);
                r.setId(50L);
                return r;
            });

            // When
            BcpcReferralResponse response = bcpcReferralService.issueReferral(1L, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(50L);
            assertThat(response.getReferredTo()).isEqualTo("LSWDO");
            then(bcpcReferralRepository).should().save(any(BcpcReferral.class));
        }

        @Test
        @DisplayName("givenNonExistentCase_whenIssueReferral_thenThrowResourceNotFoundException")
        void givenNonExistentCase_whenIssueReferral_thenThrowResourceNotFoundException() {
            // Given
            BcpcReferralCreateRequest request = new BcpcReferralCreateRequest();
            given(caseRepository.findById(999L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> bcpcReferralService.issueReferral(999L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("BCPC Case not found with ID: 999");
        }
    }

    @Nested
    @DisplayName("Get Referrals Tests")
    class GetReferralsTests {

        @Test
        @DisplayName("givenValidCaseId_whenGetReferrals_thenReturnList")
        void givenValidCaseId_whenGetReferrals_thenReturnList() {
            // Given
            Case aCase = new Case();
            aCase.setId(1L);

            BcpcReferral referral = new BcpcReferral();
            referral.setId(50L);
            referral.setReferredTo("DSWD");
            referral.setStatus("Active");
            referral.setReferralDate(LocalDateTime.now());

            given(caseRepository.existsById(1L)).willReturn(true);
            given(bcpcReferralRepository.findByCasesIdOrderByReferralDateDesc(1L)).willReturn(List.of(referral));

            // When
            List<BcpcReferralResponse> result = bcpcReferralService.getReferrals(1L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getReferredTo()).isEqualTo("DSWD");
            then(bcpcReferralRepository).should().findByCasesIdOrderByReferralDateDesc(1L);
        }
    }
}
