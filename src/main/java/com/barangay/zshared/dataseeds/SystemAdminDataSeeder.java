package com.barangay.zshared.dataseeds;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.usermanagement.permission.model.Permission;
import com.barangay.usermanagement.permission.repository.PermissionRepository;
import com.barangay.usermanagement.role.model.Role;
import com.barangay.usermanagement.role.repository.RoleRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.enumerated.MfaType;
import com.barangay.zshared.enumerated.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SystemAdminDataSeeder.java
 *
 * Initializer responsible for bootstrapping the initial System Administrator and Barangay Admin accounts.
 * Executes fourth (@Order(4)) as it depends on pre-existing Roles, Departments, and Permissions.
 * Grants department access and permissions to System Admin and Barangay Admin accounts.
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
    private final PersonRepository personRepository;
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
        seedBarangayAdmin();
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

    private void seedBarangayAdmin() {
        String targetEmail = "jeremiahmadronio2003@gmail.com";

        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseGet(() -> roleRepository.findByRoleName("SYSTEM_ADMIN")
                        .orElseThrow(() -> new RuntimeException("ADMIN role not found. Ensure RoleDataSeeder ran first.")));

        Set<Department> allDepartments = new HashSet<>(departmentRepository.findAll());
        Set<Permission> allPermissions = new HashSet<>(permissionRepository.findAll());

        Optional<User> existingOpt = userRepository.findBySystemEmail(targetEmail);

        if (existingOpt.isPresent()) {
            User admin = existingOpt.get();
            admin.setRole(adminRole);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setIsNewAccount(false);
            admin.setDepartments(allDepartments);
            admin.setCustomPermissions(allPermissions);
            userRepository.save(admin);
            log.info("Barangay Admin account ({}) updated with ADMIN role and system admin password.", targetEmail);
            return;
        }

        User admin = new User();
        admin.setSystemEmail(targetEmail);
        admin.setUsername("jeremiahmadronio");
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setStatus(UserStatus.ACTIVE);
        admin.setIsSystem(false);
        admin.setIsNewAccount(false);
        admin.setMfaType(MfaType.EMAIL);
        admin.setRole(adminRole);
        admin.setDepartments(allDepartments);
        admin.setCustomPermissions(allPermissions);

        Optional<Person> personOpt = personRepository.findAll().stream()
                .filter(p -> "Jeremiah".equalsIgnoreCase(p.getFirstName()) && "Madronio".equalsIgnoreCase(p.getLastName()))
                .findFirst();

        if (personOpt.isPresent()) {
            admin.setPerson(personOpt.get());
        } else {
            Person person = new Person();
            person.setFirstName("Jeremiah");
            person.setLastName("Madronio");
            person.setGender("Male");
            person.setBirthDate(LocalDate.of(2003, 1, 1));
            person = personRepository.save(person);
            admin.setPerson(person);
        }

        userRepository.save(admin);
        log.info("Barangay Admin created: {} with ADMIN role, linked Person Jeremiah Madronio, and system admin password.", targetEmail);
    }
}