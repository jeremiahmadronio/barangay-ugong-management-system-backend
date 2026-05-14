package com.barangay.auth.service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Base64;


/**
 * Handles TOTP secret generation, QR code creation, and code verification.
 * Used by TotpSetUpService during authenticator app setup,
 * and by AuthService during TOTP-based login verification.
 */
@Service
@RequiredArgsConstructor
public class TotpService {

    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;
    private final CodeVerifier codeVerifier;

    /** Generates a new random TOTP secret key. */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /** Returns true if the given code matches the secret at the current time window. */
    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }

    /** Generates a Base64-encoded QR code image for the given secret and email. */
    public String generateQrCodeBase64(String secret, String email) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("Barangay Ugong")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            byte[] imageData = qrGenerator.generate(data);
            return "data:image/png;base64," +
                    Base64.getEncoder().encodeToString(imageData);
        } catch (QrGenerationException e) {
            throw new RuntimeException("QR generation failed", e);
        }
    }
}