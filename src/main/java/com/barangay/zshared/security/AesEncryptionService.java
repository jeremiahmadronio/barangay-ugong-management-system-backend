package com.barangay.zshared.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Handles AES-256 encryption and decryption for sensitive data.
 * Uses AES/GCM/NoPadding mode with a random IV per encryption
 * to ensure each encrypted value is unique even for the same input.

 * The secret key is loaded from application properties on startup.
 * It must be a Base64-encoded 256-bit (32-byte) key.
 */
@Service
public class AesEncryptionService {

    // Encryption algorithm used — AES in GCM mode, no padding
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    // Length of the Initialization Vector in bytes (12 bytes is standard for GCM)
    private static final int GCM_IV_LENGTH = 12;

    // GCM authentication tag length in bits
    private static final int GCM_TAG_LENGTH = 128;

    @Value("${app.encryption.secret-key}")
    private String secretKeyBase64;

    private SecretKey secretKey;


    /**
     * Validates and loads the secret key from application properties.
     * Throws an error on startup if the key is not exactly 32 bytes (256-bit).
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AES secret key must be 256-bit (32 bytes). Got: " + keyBytes.length + " bytes.");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }


    /**
     * Encrypts a plain text string using AES-256-GCM.
     * Generates a random IV for every call and prepends it to the result.
     * Returns a Base64-encoded string containing the IV + encrypted data.
     * Returns null if the input is null.
     */
    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(plainText.getBytes());

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }


    /**
     * Decrypts a Base64-encoded string that was encrypted by this service.
     * Extracts the IV from the first 12 bytes, then decrypts the rest.
     * Returns null if the input is null.
     */
    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);

            if (combined.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted data — too short.");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(cipherText));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }


    /**
     * Safe version of encrypt — skips encryption if the value is null or blank.
     * Used by the JPA converter to avoid errors on empty fields.
     */
    public String encryptSafe(String value) {
        if (value == null || value.isBlank()) return value;
        return encrypt(value);
    }

    /**
     * Safe version of decrypt — skips decryption if the value is null or blank.
     * Used by the JPA converter to avoid errors on empty fields.
     */
    public String decryptSafe(String value) {
        if (value == null || value.isBlank()) return value;
        return decrypt(value);
    }


    /**
     * Checks if a value looks like it was already encrypted by this service.
     * Tries to Base64-decode it and checks if the length is longer than the IV.
     * Not 100% guaranteed but good enough for a basic guard check.
     */
    public boolean isEncrypted(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length > GCM_IV_LENGTH;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Encrypts the value only if it is not already encrypted.
     * Useful when re-saving existing records to avoid double encryption.
     */
    public String encryptIfNotEncrypted(String value) {
        if (value == null || value.isBlank()) return value;
        return isEncrypted(value) ? value : encrypt(value);
    }


    /**
     * Utility method to generate a new random Base64-encoded 256-bit AES key.
     * Use this once to generate the key for your application properties.
     */
    public static String generateBase64Key() {
        byte[] key = new byte[32]; // 256-bit
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}