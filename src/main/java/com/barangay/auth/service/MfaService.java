package com.barangay.auth.service;

import com.barangay.zshared.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Handles OTP generation and email delivery for MFA verification.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MfaService {

    private final EmailService emailService;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** Generates a cryptographically secure random 6-digit OTP code (100000 - 999999). */
    public String generateCode() {
        int code = 100_000 + SECURE_RANDOM.nextInt(900_000);
        return String.valueOf(code);
    }

    /** Sends the OTP code to the given email address. */
    public void sendMfaEmail(String toEmail, String code) {
        log.info("Sending MFA OTP verification code to recipient");
        try {
            emailService.sendMfaEmail(toEmail, code);
        } catch (Exception e) {
            log.warn("Failed to send email via SMTP, but MFA code is generated: {}", e.getMessage());
        }
    }
}