package com.barangay.zshared.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AesEncryptionService BDD Unit Tests")
class AesEncryptionServiceTest {

    private AesEncryptionService aesEncryptionService;

    // Standard 32-byte (256-bit) test secret key encoded in Base64
    private static final String VALID_KEY_BASE64 = Base64.getEncoder().encodeToString(
            "01234567890123456789012345678901".getBytes()
    );

    @BeforeEach
    void setUp() {
        aesEncryptionService = new AesEncryptionService();
        ReflectionTestUtils.setField(aesEncryptionService, "secretKeyBase64", VALID_KEY_BASE64);
        aesEncryptionService.init();
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when secret key is not 32 bytes")
        void givenInvalidKeyLength_whenInit_thenThrowsException() {
            // Given
            AesEncryptionService invalidService = new AesEncryptionService();
            String shortKey = Base64.getEncoder().encodeToString("short-key".getBytes());
            ReflectionTestUtils.setField(invalidService, "secretKeyBase64", shortKey);

            // When & Then
            assertThatThrownBy(invalidService::init)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("AES secret key must be 256-bit (32 bytes)");
        }
    }

    @Nested
    @DisplayName("Encryption & Decryption Round-Trip Tests")
    class RoundTripTests {

        @Test
        @DisplayName("Should successfully encrypt and decrypt a plain text string")
        void givenPlainText_whenEncryptAndDecrypt_thenReturnsOriginalText() {
            // Given
            String original = "Juan Dela Cruz - Confidential Blotter Details";

            // When
            String encrypted = aesEncryptionService.encrypt(original);
            String decrypted = aesEncryptionService.decrypt(encrypted);

            // Then
            assertThat(encrypted).isNotNull().isNotEqualTo(original);
            assertThat(decrypted).isEqualTo(original);
        }

        @Test
        @DisplayName("Should return null when encrypting null text")
        void givenNullPlainText_whenEncrypt_thenReturnNull() {
            // When
            String encrypted = aesEncryptionService.encrypt(null);

            // Then
            assertThat(encrypted).isNull();
        }

        @Test
        @DisplayName("Should return null when decrypting null text")
        void givenNullCipherText_whenDecrypt_thenReturnNull() {
            // When
            String decrypted = aesEncryptionService.decrypt(null);

            // Then
            assertThat(decrypted).isNull();
        }

        @Test
        @DisplayName("Should produce different cipher texts for same input due to unique IVs")
        void givenSamePlainText_whenEncryptedTwice_thenProducesDifferentCipherTexts() {
            // Given
            String text = "SecretData123";

            // When
            String encrypted1 = aesEncryptionService.encrypt(text);
            String encrypted2 = aesEncryptionService.encrypt(text);

            // Then
            assertThat(encrypted1).isNotEqualTo(encrypted2);
            assertThat(aesEncryptionService.decrypt(encrypted1)).isEqualTo(text);
            assertThat(aesEncryptionService.decrypt(encrypted2)).isEqualTo(text);
        }
    }

    @Nested
    @DisplayName("Decryption Error Handling Tests")
    class DecryptionErrorTests {

        @Test
        @DisplayName("Should throw RuntimeException when ciphertext is invalid or tampered")
        void givenTamperedCipherText_whenDecrypt_thenThrowsRuntimeException() {
            // Given
            String invalidBase64 = Base64.getEncoder().encodeToString("corrupted-data-not-gcm".getBytes());

            // When & Then
            assertThatThrownBy(() -> aesEncryptionService.decrypt(invalidBase64))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Decryption failed");
        }
    }
}
