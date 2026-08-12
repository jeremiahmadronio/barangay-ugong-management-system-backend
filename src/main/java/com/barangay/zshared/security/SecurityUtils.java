package com.barangay.zshared.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.barangay.usermanagement.user.model.User;

import lombok.extern.slf4j.Slf4j;

/**
 * Shared security utility for retrieving authenticated user context.
 */
@Slf4j
@Component
public class SecurityUtils {

    /**
     * Extracts the logged-in {@link User} entity from Spring SecurityContextHolder.
     *
     * @return the authenticated {@link User}, or {@code null} if unauthenticated
     */
    public static User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails(User user)) {
                return user;
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve authenticated user from SecurityContext: {}", e.getMessage());
        }
        return null;
    }
}
