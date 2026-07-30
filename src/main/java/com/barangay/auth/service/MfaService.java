package com.barangay.auth.service;

import com.barangay.zshared.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Handles OTP generation and email delivery for MFA verification.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MfaService {

    private final EmailService emailService;

    /** Generates a random 6-digit OTP code. */
    public String generateCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(999999));
    }

    /** Sends the OTP code to the given email address. */
    public void sendMfaEmail(String toEmail, String code) {
        log.info(">>> MFA OTP CODE FOR DEV TEST [{}]: {} <<<", toEmail, code);
        try {
            emailService.sendMfaEmail(toEmail, code);
        } catch (Exception e) {
            log.warn("Failed to send email via SMTP, but MFA code is generated: {}", e.getMessage());
        }
    }
}