package com.barangay.auth.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("TotpService BDD Unit Tests")
class TotpServiceTest {

    @InjectMocks
    private TotpService totpService;

    @Mock
    private SecretGenerator secretGenerator;

    @Mock
    private QrGenerator qrGenerator;

    @Mock
    private CodeVerifier codeVerifier;

    @Nested
    @DisplayName("generateSecret Tests")
    class GenerateSecretTests {

        @Test
        @DisplayName("Should generate random secret from SecretGenerator")
        void givenCall_whenGenerateSecret_thenReturnGeneratedSecret() {
            // Given
            given(secretGenerator.generate()).willReturn("JBSWY3DPEHPK3PXP");

            // When
            String secret = totpService.generateSecret();

            // Then
            assertThat(secret).isEqualTo("JBSWY3DPEHPK3PXP");
        }
    }

    @Nested
    @DisplayName("verifyCode Tests")
    class VerifyCodeTests {

        @Test
        @DisplayName("Should return true when CodeVerifier validates code successfully")
        void givenValidCode_whenVerifyCode_thenReturnTrue() {
            // Given
            String secret = "JBSWY3DPEHPK3PXP";
            String code = "123456";
            given(codeVerifier.isValidCode(secret, code)).willReturn(true);

            // When
            boolean valid = totpService.verifyCode(secret, code);

            // Then
            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("Should return false when CodeVerifier rejects invalid code")
        void givenInvalidCode_whenVerifyCode_thenReturnFalse() {
            // Given
            String secret = "JBSWY3DPEHPK3PXP";
            String code = "000000";
            given(codeVerifier.isValidCode(secret, code)).willReturn(false);

            // When
            boolean valid = totpService.verifyCode(secret, code);

            // Then
            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("generateQrCodeBase64 Tests")
    class GenerateQrCodeTests {

        @Test
        @DisplayName("Should generate valid Base64 data URL string for QR code image")
        void givenSecretAndEmail_whenGenerateQrCode_thenReturnBase64DataUrl() throws Exception {
            // Given
            byte[] mockPngBytes = "fake-qr-png-bytes".getBytes();
            given(qrGenerator.generate(any(QrData.class))).willReturn(mockPngBytes);

            // When
            String qrUrl = totpService.generateQrCodeBase64("SECRET123", "admin@barangay.gov.ph");

            // Then
            assertThat(qrUrl)
                    .startsWith("data:image/png;base64,")
                    .contains(Base64.getEncoder().encodeToString(mockPngBytes));
        }

        @Test
        @DisplayName("Should throw RuntimeException when QrGenerator encounters error")
        void givenQrGeneratorThrows_whenGenerateQrCode_thenThrowsRuntimeException() throws Exception {
            // Given
            given(qrGenerator.generate(any(QrData.class)))
                    .willThrow(new QrGenerationException("Generation failed", null));

            // When & Then
            assertThatThrownBy(() -> totpService.generateQrCodeBase64("SECRET123", "user@barangay.gov.ph"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("QR generation failed");
        }
    }
}
