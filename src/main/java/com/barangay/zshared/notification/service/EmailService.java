package com.barangay.zshared.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Shared central service for sending system email notifications.
 * Handles MFA OTPs, new account credential notifications, and generic emails.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String configuredSender;

    private static final String DEFAULT_SENDER = "noreply@barangay.gov.ph";
    private static final String SYSTEM_NAME = "Barangay Management System";

    /**
     * Sends the 6-digit MFA OTP code to the target email.
     */
    @Async
    public void sendMfaEmail(String toEmail, String code) {
        String subject = SYSTEM_NAME + " – Verification Code";
        String content = "Good day,\n\n" +
                "You are receiving this email because a verification request was made for your account.\n\n" +
                "Your 6-digit verification code is:\n\n" +
                code + "\n\n" +
                "This code will expire in 5 minutes. Please do not share this code with anyone.\n\n" +
                "If you did not request this verification, please ignore this email.\n\n" +
                "Thank you,\n" +
                SYSTEM_NAME;

        sendEmail(toEmail, subject, content);
    }

    /**
     * Sends initial login credentials (default username & temporary password)
     * to a newly created user account.
     */
    @Async
    public void sendAccountCredentialsEmail(String toEmail, String username, String tempPassword) {
        String subject = SYSTEM_NAME + " – Account Created";
        String content = "Good day,\n\n" +
                "Your user account has been successfully created for the " + SYSTEM_NAME + ".\n\n" +
                "Your login credentials are:\n" +
                "Temporary Password: " + tempPassword + "\n\n" +
                "Please log in using these credentials. You will be prompted to set up a new password upon first login.\n\n" +
                "Thank you,\n" +
                SYSTEM_NAME;

        sendEmail(toEmail, subject, content);
    }

    /**
     * Generic helper for dispatching simple text emails.
     */
    public void sendEmail(String toEmail, String subject, String textContent) {
        try {
            String fromAddress = (configuredSender != null && !configuredSender.isBlank()) ? configuredSender : DEFAULT_SENDER;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(textContent);

            mailSender.send(message);
            log.info("Email successfully sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
