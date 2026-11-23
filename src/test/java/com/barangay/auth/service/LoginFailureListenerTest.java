package com.barangay.auth.service;

import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginFailureListener BDD Unit Tests")
class LoginFailureListenerTest {

    @InjectMocks
    private LoginFailureListener loginFailureListener;

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("onAuthenticationFailure Tests")
    class AuthenticationFailureTests {

        @Test
        @DisplayName("Should increment failed attempts count when user exists and attempts < 5")
        void givenExistingUser_whenAuthFails_thenIncrementsFailedAttempts() {
            // Given
            String email = "officer@barangay.gov.ph";
            User user = new User();
            user.setSystemEmail(email);
            user.setFailedAttempts(2);
            user.setIsLocked(false);

            given(userRepository.findBySystemEmail(email)).willReturn(Optional.of(user));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(email, "wrong-pass");
            AuthenticationFailureBadCredentialsEvent event =
                    new AuthenticationFailureBadCredentialsEvent(auth, new BadCredentialsException("Bad credentials"));

            // When
            loginFailureListener.onAuthenticationFailure(event);

            // Then
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().saveAndFlush(captor.capture());

            User savedUser = captor.getValue();
            assertThat(savedUser.getFailedAttempts()).isEqualTo(3);
            assertThat(savedUser.getIsLocked()).isFalse();
        }

        @Test
        @DisplayName("Should lock account when failed attempts reach 5 threshold")
        void givenUserWith4Attempts_whenAuthFails_thenLocksAccount() {
            // Given
            String email = "staff@barangay.gov.ph";
            User user = new User();
            user.setSystemEmail(email);
            user.setFailedAttempts(4);
            user.setIsLocked(false);

            given(userRepository.findBySystemEmail(email)).willReturn(Optional.of(user));

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(email, "wrong-pass");
            AuthenticationFailureBadCredentialsEvent event =
                    new AuthenticationFailureBadCredentialsEvent(auth, new BadCredentialsException("Bad credentials"));

            // When
            loginFailureListener.onAuthenticationFailure(event);

            // Then
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().saveAndFlush(captor.capture());

            User savedUser = captor.getValue();
            assertThat(savedUser.getFailedAttempts()).isEqualTo(5);
            assertThat(savedUser.getIsLocked()).isTrue();
            assertThat(savedUser.getLockUntil()).isNotNull();
        }

        @Test
        @DisplayName("Should ignore event gracefully when user is not found in database")
        void givenUnknownEmail_whenAuthFails_thenDoesNothing() {
            // Given
            String email = "nonexistent@barangay.gov.ph";
            given(userRepository.findBySystemEmail(email)).willReturn(Optional.empty());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(email, "wrong-pass");
            AuthenticationFailureBadCredentialsEvent event =
                    new AuthenticationFailureBadCredentialsEvent(auth, new BadCredentialsException("Bad credentials"));

            // When
            loginFailureListener.onAuthenticationFailure(event);

            // Then
            then(userRepository).should(never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
        }
    }
}
