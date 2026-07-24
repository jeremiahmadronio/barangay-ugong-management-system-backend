package com.barangay.zshared.data_seeds;

import com.barangay.user_management.permission.model.Permission;
import com.barangay.user_management.permission.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PermissionDataSeeder.java
 * Initializer responsible for seeding system permissions categorized by module.
 * Executes first (@Order(1)) alongside roles to ensure permissions exist for access assignment.
 */
@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class PermissionDataSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissions();
    }

    private void seedPermissions() {
        // BLOTTER MODULE PERMISSIONS
        createPermIfNotFound("View Records", "BLOTTER", "Allows viewing blotter case records");
        createPermIfNotFound("Manage Mediation", "BLOTTER", "Allows managing mediation processes");
        createPermIfNotFound("Manage Lupon Escalation", "BLOTTER", "Allows escalating blotter cases to Lupon");

        // GLOBAL / CASE MANAGEMENT PERMISSIONS
        createPermIfNotFound("View Cases", "GLOBAL", "Allows viewing system cases");
        createPermIfNotFound("Create Case Entry", "GLOBAL", "Allows creating new case entries");
        createPermIfNotFound("Archive Cases", "GLOBAL", "Allows archiving inactive or closed cases");
        createPermIfNotFound("Resolve & Finalize Case", "GLOBAL", "Allows resolving and finalizing cases");
        createPermIfNotFound("Manage Case notes", "GLOBAL", "Allows adding and managing case notes");
        createPermIfNotFound("Manage Reports", "GLOBAL", "Allows generating and managing module reports");
        createPermIfNotFound("Issue Referral", "GLOBAL", "Allows issuing external or internal case referrals");
        createPermIfNotFound("Update Case information", "GLOBAL", "Allows modifying case details");

        // LUPON MODULE PERMISSIONS
        createPermIfNotFound("Manage Conciliation", "LUPON", "Allows managing conciliation proceedings");

        // VAWC MODULE PERMISSIONS
        createPermIfNotFound("Issue BPO", "VAWC", "Allows issuing Barangay Protection Orders");
        createPermIfNotFound("Manage Intervention", "VAWC", "Allows managing VAWC social interventions");

        // CLEARANCE MODULE PERMISSIONS
        createPermIfNotFound("Issue Clearance", "CLEARANCE", "Allows issuing barangay clearances");
        createPermIfNotFound("Edit Template", "CLEARANCE", "Allows editing clearance document templates");
        createPermIfNotFound("View Revenue Reports", "CLEARANCE", "Allows viewing clearance revenue reports");

        // FTJS (FIRST TIME JOB SEEKERS) PERMISSIONS
        createPermIfNotFound("Register new Applicant", "FTJS", "Allows registering first-time job seeker applicants");
        createPermIfNotFound("Issue ftjs Certificate", "FTJS", "Allows issuing FTJS certificates");
        createPermIfNotFound("View ftjs Records", "FTJS", "Allows viewing FTJS registry records");
        createPermIfNotFound("Update Applicant Info", "FTJS", "Allows updating FTJS applicant details");

        // USER & RESIDENT MANAGEMENT PERMISSIONS
        createPermIfNotFound("Create user", "USER_MANAGEMENT", "Allows creating system user accounts");
        createPermIfNotFound("View users", "USER_MANAGEMENT", "Allows viewing user accounts list");
        createPermIfNotFound("Edit users", "USER_MANAGEMENT", "Allows modifying user account details");
        createPermIfNotFound("Lock users", "USER_MANAGEMENT", "Allows locking user accounts");
        createPermIfNotFound("Archive users", "USER_MANAGEMENT", "Allows archiving user accounts");

        createPermIfNotFound("Create resident", "RESIDENT_MANAGEMENT", "Allows registering new residents");
        createPermIfNotFound("View residents", "RESIDENT_MANAGEMENT", "Allows viewing resident profiles");
        createPermIfNotFound("Edit resident", "RESIDENT_MANAGEMENT", "Allows updating resident profiles");
        createPermIfNotFound("Archive resident", "RESIDENT_MANAGEMENT", "Allows archiving resident records");

        createPermIfNotFound("Create officer", "OFFICER_MANAGEMENT", "Allows creating barangay officer profiles");
        createPermIfNotFound("View officers", "OFFICER_MANAGEMENT", "Allows viewing barangay officers list");
        createPermIfNotFound("Edit officer", "OFFICER_MANAGEMENT", "Allows updating barangay officer details");
        createPermIfNotFound("Restore Archive", "SYSTEM", "Allows restoring archived records");
    }

    /**
     * Helper method to create permission if it does not already exist.
     */
    private void createPermIfNotFound(String name, String module, String description) {
        String cleanName = name.trim();
        if (!permissionRepository.existsByPermissionName(cleanName)) {
            Permission perm = new Permission();
            perm.setPermissionName(cleanName);
            perm.setModule(module);
            perm.setDescription(description);
            permissionRepository.save(perm);
            log.info("Seeded permission: [{}] in module [{}]", cleanName, module);
        }
    }
}
