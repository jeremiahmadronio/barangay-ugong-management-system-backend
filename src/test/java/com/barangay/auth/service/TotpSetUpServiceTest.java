package com.barangay.auth.service;

import com.barangay.auth.dto.TotpConfirmationRequest;
import com.barangay.auth.dto.TotpEnableSuccessResponse;
import com.barangay.auth.dto.TotpSetupResponse;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.audit.service.AuditLogService;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("TotpSetUpService BDD Unit Tests")
class TotpSetUpServiceTest {

    @InjectMocks
    private TotpSetUpService totpSetUpService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TotpService totpService;

    @Mock
    private AuditLogService auditLogService;

    @Nested
    @DisplayName("initiateTotpSetup Tests")
    class InitiateTotpSetupTests {

        @Test
        @DisplayName("Should generate secret and QR code for authenticated user")
        void givenValidUserId_whenInitiateTotpSetup_thenReturnSetupResponse() {
            // Given
            Long userId = 10L;
            User user = new User();
            user.setId(userId);
            user.setSystemEmail("staff@barangay.gov.ph");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(totpService.generateSecret()).willReturn("SECRETKEY123");
            given(totpService.generateQrCodeBase64("SECRETKEY123", "staff@barangay.gov.ph"))
                    .willReturn("data:image/png;base64,mockQrCode");

            // When
            TotpSetupResponse response = totpSetUpService.initiateTotpSetup(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.secret()).isEqualTo("SECRETKEY123");
            assertThat(response.qrCode()).isEqualTo("data:image/png;base64,mockQrCode");
        }

        @Test
        @DisplayName("Should throw RuntimeException when user does not exist")
        void givenUnknownUser_whenInitiateTotpSetup_thenThrowsException() {
            // Given
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> totpSetUpService.initiateTotpSetup(99L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }
    }

    @Nested
    @DisplayName("confirmAndEnable Tests")
    class ConfirmAndEnableTests {

        @Test
        @DisplayName("Should enable TOTP and save secret when code is valid")
        void givenValidCode_whenConfirmAndEnable_thenEnablesTotpAndLogsAudit() {
            // Given
            Long userId = 5L;
            User user = new User();
            user.setId(userId);
            user.setSystemEmail("admin@barangay.gov.ph");
            user.setTotpEnabled(false);

            TotpConfirmationRequest request = new TotpConfirmationRequest("SECRETXYZ", "654321");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(totpService.verifyCode("SECRETXYZ", "654321")).willReturn(true);

            // When
            TotpEnableSuccessResponse response = totpSetUpService.confirmAndEnable(userId, request, "127.0.0.1");

            // Then
            assertThat(response).isNotNull();
            assertThat(user.getTotpEnabled()).isTrue();
            assertThat(user.getTotpSecret()).isEqualTo("SECRETXYZ");

            then(userRepository).should().save(user);
            then(auditLogService).should().log(
                    eq(user),
                    eq(AuditModule.SECURITY),
                    eq("TOTP_ENABLED"),
                    eq(Severity.INFO),
                    eq("User enabled authenticator app"),
                    eq("127.0.0.1"),
                    isNull(),
                    isNull()
            );
        }

        @Test
        @DisplayName("Should throw BadCredentialsException when verification code is invalid")
        void givenInvalidCode_whenConfirmAndEnable_thenThrowsBadCredentialsException() {
            // Given
            Long userId = 5L;
            User user = new User();
            user.setId(userId);

            TotpConfirmationRequest request = new TotpConfirmationRequest("SECRETXYZ", "000000");
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(totpService.verifyCode("SECRETXYZ", "000000")).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> totpSetUpService.confirmAndEnable(userId, request, "127.0.0.1"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Invalid verification code");
        }
    }

    @Nested
    @DisplayName("disableTotp Tests")
    class DisableTotpTests {

        @Test
        @DisplayName("Should disable TOTP and clear secret for authenticated user")
        void givenValidUserId_whenDisableTotp_thenClearsSecretAndLogsAudit() {
            // Given
            Long userId = 8L;
            User user = new User();
            user.setId(userId);
            user.setTotpEnabled(true);
            user.setTotpSecret("EXISTING_SECRET");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // When
            totpSetUpService.disableTotp(userId, "192.168.1.1");

            // Then
            assertThat(user.getTotpEnabled()).isFalse();
            assertThat(user.getTotpSecret()).isNull();

            then(userRepository).should().save(user);
            then(auditLogService).should().log(
                    eq(user),
                    eq(AuditModule.SECURITY),
                    eq("TOTP_DISABLED"),
                    eq(Severity.WARNING),
                    eq("User disabled authenticator app"),
                    eq("192.168.1.1"),
                    isNull(),
                    isNull()
            );
        }
    }
}
