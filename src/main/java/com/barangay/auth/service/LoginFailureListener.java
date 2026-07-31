package com.barangay.auth.service;

import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class LoginFailureListener {

    private final UserRepository userRepository;

    /**
     * Listens for Spring Security authentication failure events.
     * Acts as a safety net — runs in a separate transaction so it
     * always saves even if the main request transaction fails.
     */
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuthenticationFailure(AuthenticationFailureBadCredentialsEvent event) {
        String email = (String) event.getAuthentication().getPrincipal();
        User user = userRepository.findBySystemEmail(email).orElse(null);

        if (user != null) {
            int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
            user.setFailedAttempts(attempts);

            if (attempts >= 10) {
                user.setIsLocked(true);
                user.setLockUntil(LocalDateTime.now().plusMinutes(15));
            }

            userRepository.saveAndFlush(user);
        }
    }
}
