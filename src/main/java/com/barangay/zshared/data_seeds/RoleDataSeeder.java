package com.barangay.zshared.data_seeds;

import com.barangay.user_management.role.model.Role;
import com.barangay.user_management.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


/**
 * RoleDataSeeder.java
 * Initializer responsible for seeding default system roles into the database.
 * Executes first (@Order(1)) to ensure roles exist before assigning them to users.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class RoleDataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;



    @Override
    public void run(String... args) {
        seedRole("SYSTEM_ADMIN", "Full access across all system features and configurations");
        seedRole("ADMIN", "Administrative access to overall barangay modules");
        seedRole("OFFICER", "Department-specific operational access (Blotter, Lupon, VAWC, etc.)");
    }
    private void seedRole(String roleName, String description) {
        if (!roleRepository.existsByRoleName(roleName)) {
            Role role = new Role();
            role.setRoleName(roleName);
            role.setDescription(description);
            role.setIsSystem(true);
            roleRepository.save(role);
        }
    }
}
