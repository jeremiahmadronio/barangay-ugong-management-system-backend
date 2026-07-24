package com.barangay.auth.service;

import com.barangay.zshared.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Handles OTP generation and email delivery for MFA verification.
 */
@Service
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
        emailService.sendMfaEmail(toEmail, code);
    }
}