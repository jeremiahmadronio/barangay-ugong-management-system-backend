package com.barangay.zshared.data_seeds;

import com.barangay.user_management.department.model.Department;
import com.barangay.user_management.department.repository.DepartmentRepository;
import com.barangay.user_management.permission.model.Permission;
import com.barangay.user_management.permission.repository.PermissionRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * SystemAdminDataSeeder.java
 *
 * Initializer responsible for bootstrapping the initial System Administrator account.
 * Executes fourth (@Order(4)) as it depends on pre-existing Roles, Departments, and Permissions.
 * Grants ALL department access and ALL system permissions to the System Admin.
 */
@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class SystemAdminDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.setup.admin-email}")
    private String adminEmail;

    @Value("${app.setup.admin-password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        updateResidentCheckConstraint();
        seedSystemAdmin();
    }

    private void updateResidentCheckConstraint() {
        try {
            jdbcTemplate.execute("ALTER TABLE resident DROP CONSTRAINT IF EXISTS resident_status_check;");
            jdbcTemplate.execute("ALTER TABLE resident ADD CONSTRAINT resident_status_check CHECK (status IN ('ACTIVE', 'INACTIVE', 'DECEASED', 'MOVED_OUT', 'ARCHIVED'));");
            log.info("Successfully updated resident_status_check constraint to include ARCHIVED status.");
        } catch (Exception e) {
            log.warn("Could not alter resident_status_check constraint: {}", e.getMessage());
        }
    }

    private void seedSystemAdmin() {
        Role rootAdminRole = roleRepository.findByRoleName("SYSTEM_ADMIN")
                .orElseThrow(() -> new RuntimeException("SYSTEM_ADMIN role not found. Ensure RoleDataSeeder ran first."));

        Set<Department> allDepartments = new HashSet<>(departmentRepository.findAll());
        Set<Permission> allPermissions = new HashSet<>(permissionRepository.findAll());

        Optional<User> existingAdminOpt = userRepository.findBySystemEmail(adminEmail);

        if (existingAdminOpt.isPresent()) {
            User admin = existingAdminOpt.get();
            admin.setDepartments(allDepartments);
            admin.setCustomPermissions(allPermissions);
            userRepository.save(admin);
            log.info("System Admin already exists. Updated with ALL ({}) departments and ALL ({}) permissions.",
                    allDepartments.size(), allPermissions.size());
            return;
        }

        User admin = new User();
        admin.setSystemEmail(adminEmail);
        admin.setUsername("superadmin");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setStatus(UserStatus.ACTIVE);
        admin.setIsSystem(true);
        admin.setIsNewAccount(false);
        admin.setMfaType(MfaType.EMAIL);
        admin.setRole(rootAdminRole);
        admin.setDepartments(allDepartments);
        admin.setCustomPermissions(allPermissions);

        userRepository.save(admin);
        log.info("System Admin created: {} with ALL ({}) departments and ALL ({}) permissions.",
                adminEmail, allDepartments.size(), allPermissions.size());
    }
}