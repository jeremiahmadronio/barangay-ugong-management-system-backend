package com.barangay.user_management.user.configuration;


import com.barangay.user_management.department.model.Department;
import com.barangay.user_management.department.repository.DepartmentRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.setup.admin-email}")
    private String adminEmail;

    @Value("${app.setup.admin-password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedDepartments();
        seedSystemAdmin();
    }


    private void seedRoles() {
        List<String> roles = List.of(
                "ROOT_ADMIN",
                "KAPITANA",
                "ADMIN",
                "STAFF"
        );

        roles.forEach(name -> {
            if (roleRepository.findByRoleName(name).isEmpty()) {
                Role role = new Role();
                role.setRoleName(name);
                roleRepository.save(role);
                log.info("Seeded role: {}", name);
            }
        });
    }


    private void seedDepartments() {
        List<String> departments = List.of(
                "BLOTTER",
                "LUPON",
                "VAWC",
                "BCPC",
                "CLEARANCE",
                "FTJS"
        );

        departments.forEach(name -> {
            if (departmentRepository.findByName(name).isEmpty()) {
                Department dept = new Department();
                dept.setName(name);
                dept.setIsActive(true);
                departmentRepository.save(dept);
                log.info("Seeded department: {}", name);
            }
        });
    }


    private void seedSystemAdmin() {
        if (userRepository.findBySystemEmail(adminEmail).isPresent()) {
            log.info("System Admin already exists. Skipping.");
            return;
        }

        Role rootAdminRole = roleRepository.findByRoleName("ROOT_ADMIN")
                .orElseThrow(() -> new RuntimeException("ROOT_ADMIN role not found. Make sure seedRoles() ran first."));

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