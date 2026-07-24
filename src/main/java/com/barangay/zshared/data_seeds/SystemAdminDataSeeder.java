package com.barangay.zshared.data_seeds;

import com.barangay.user_management.role.model.Role;
import com.barangay.user_management.role.repository.RoleRepository;
import com.barangay.user_management.user.model.User;
import com.barangay.user_management.user.repository.UserRepository;
import com.barangay.zshared.enumerated.MfaType;
import com.barangay.zshared.enumerated.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


/**
 * SystemAdminDataSeeder.java
 *
 * Initializer responsible for bootstrapping the initial System Administrator account.
 * Executes third (@Order(3)) as it depends on pre-existing Roles generated in Order 1.
 */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
public class SystemAdminDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.setup.admin-email}")
    private String adminEmail;

    @Value("${app.setup.admin-password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        seedSystemAdmin();
    }

    private void seedSystemAdmin() {
        if (userRepository.findBySystemEmail(adminEmail).isPresent()) {
            log.info("System Admin already exists. Skipping.");
            return;
        }

        Role rootAdminRole = roleRepository.findByRoleName("SYSTEM_ADMIN")
                .orElseThrow(() -> new RuntimeException("SYSTEM_ADMIN role not found. Ensure RoleDataSeeder ran first."));

        User admin = new User();
        admin.setSystemEmail(adminEmail);
        admin.setUsername("superadmin");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setStatus(UserStatus.ACTIVE);
        admin.setIsSystem(true);
        admin.setIsNewAccount(false);
        admin.setMfaType(MfaType.EMAIL);
        admin.setRole(rootAdminRole);

        userRepository.save(admin);
        log.info("System Admin created: {}", adminEmail);
    }
}