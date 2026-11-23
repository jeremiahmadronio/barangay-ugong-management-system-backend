package com.barangay.auth.service;

import com.barangay.zshared.notification.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("MfaService BDD Unit Tests")
class MfaServiceTest {

    @InjectMocks
    private MfaService mfaService;

    @Mock
    private EmailService emailService;

    @Nested
    @DisplayName("generateCode Tests")
    class GenerateCodeTests {

        @Test
        @DisplayName("Should generate a valid 6-digit numeric OTP code")
        void givenCall_whenGenerateCode_thenReturnSixDigitCode() {
            // When
            String code = mfaService.generateCode();

            // Then
            assertThat(code)
                    .isNotNull()
                    .hasSize(6)
                    .matches("^[1-9][0-9]{5}$");
        }

        @Test
        @DisplayName("Should generate different codes across multiple invocations")
        void givenMultipleCalls_whenGenerateCode_thenOutputsVary() {
            // When
            String code1 = mfaService.generateCode();
            String code2 = mfaService.generateCode();
            String code3 = mfaService.generateCode();

            // Then: At least one code should differ given 900,000 range entropy
            assertThat(code1.equals(code2) && code2.equals(code3)).isFalse();
        }
    }

    @Nested
    @DisplayName("sendMfaEmail Tests")
    class SendMfaEmailTests {

        @Test
        @DisplayName("Should delegate MFA email dispatch to EmailService")
        void givenRecipientAndCode_whenSendMfaEmail_thenCallsEmailService() {
            // Given
            String toEmail = "resident@barangay.gov.ph";
            String code = "654321";

            // When
            mfaService.sendMfaEmail(toEmail, code);

            // Then
            then(emailService).should().sendMfaEmail(toEmail, code);
        }

        @Test
        @DisplayName("Should handle EmailService exception gracefully without throwing")
        void givenEmailServiceFails_whenSendMfaEmail_thenHandlesGracefully() {
            // Given
            String toEmail = "fail@barangay.gov.ph";
            String code = "123456";
            willThrow(new RuntimeException("SMTP connection timeout"))
                    .given(emailService).sendMfaEmail(toEmail, code);

            // When / Then: Should not propagate exception
            mfaService.sendMfaEmail(toEmail, code);
            then(emailService).should().sendMfaEmail(toEmail, code);
        }
    }
}
