package com.barangay.zshared.security;

import com.barangay.usermanagement.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Tells Spring Security how to load a user from the database.
 * This is the implementation of Spring's built-in UserDetailsService interface.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Finds a user by their system email.
     * Also loads their role and departments in one query to avoid lazy loading issues.
     * Throws UsernameNotFoundException if the email does not exist in the database.
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmailWithRoleAndDepartments(email)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + email
                ));
    }
}
