package com.barangay.zshared.notification.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

/**
 * BDD Unit tests for {@link EmailService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService BDD Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Nested
    @DisplayName("Send MFA Email Tests")
    class SendMfaEmailTests {

        @Test
        @DisplayName("givenMfaCode_whenSendMfaEmail_thenDispatchVerificationMessage")
        void givenMfaCode_whenSendMfaEmail_thenDispatchVerificationMessage() {
            // Given
            String toEmail = "citizen@example.com";
            String code = "123456";
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

            // When
            emailService.sendMfaEmail(toEmail, code);

            // Then
            then(mailSender).should().send(messageCaptor.capture());
            SimpleMailMessage sentMessage = messageCaptor.getValue();

            assertThat(sentMessage.getTo()).containsExactly(toEmail);
            assertThat(sentMessage.getSubject()).contains("Verification Code");
            assertThat(sentMessage.getText()).contains(code);
            assertThat(sentMessage.getText()).contains("expire in 5 minutes");
        }
    }

    @Nested
    @DisplayName("Send Account Credentials Email Tests")
    class SendAccountCredentialsEmailTests {

        @Test
        @DisplayName("givenCredentials_whenSendAccountCredentialsEmail_thenDispatchCredentialsMessage")
        void givenCredentials_whenSendAccountCredentialsEmail_thenDispatchCredentialsMessage() {
            // Given
            String toEmail = "newuser@example.com";
            String username = "johndoe";
            String tempPassword = "TempPassword123!";
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

            // When
            emailService.sendAccountCredentialsEmail(toEmail, username, tempPassword);

            // Then
            then(mailSender).should().send(messageCaptor.capture());
            SimpleMailMessage sentMessage = messageCaptor.getValue();

            assertThat(sentMessage.getTo()).containsExactly(toEmail);
            assertThat(sentMessage.getSubject()).contains("Account Created");
            assertThat(sentMessage.getText()).contains(tempPassword);
            assertThat(sentMessage.getText()).contains("prompted to set up a new password");
        }
    }

    @Nested
    @DisplayName("Generic Send Email Tests")
    class GenericSendEmailTests {

        @Test
        @DisplayName("givenValidEmailDetails_whenSendEmail_thenInvokeMailSenderSuccessfully")
        void givenValidEmailDetails_whenSendEmail_thenInvokeMailSenderSuccessfully() {
            // Given
            String toEmail = "resident@example.com";
            String subject = "Document Ready";
            String content = "Your requested barangay clearance is now ready for pickup.";
            ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

            // When
            emailService.sendEmail(toEmail, subject, content);

            // Then
            then(mailSender).should().send(messageCaptor.capture());
            SimpleMailMessage sentMessage = messageCaptor.getValue();

            assertThat(Objects.requireNonNull(sentMessage.getTo())[0]).isEqualTo(toEmail);
            assertThat(sentMessage.getSubject()).isEqualTo(subject);
            assertThat(sentMessage.getText()).isEqualTo(content);
        }

        @Test
        @DisplayName("givenMailSenderThrowsException_whenSendEmail_thenHandleGracefullyWithoutPropagating")
        void givenMailSenderThrowsException_whenSendEmail_thenHandleGracefullyWithoutPropagating() {
            // Given
            doThrow(new MailSendException("SMTP connection timed out"))
                    .when(mailSender).send(any(SimpleMailMessage.class));

            // When & Then
            assertThatCode(() -> emailService.sendEmail("fail@example.com", "Test Subject", "Test Body"))
                    .doesNotThrowAnyException();
        }
    }
}
