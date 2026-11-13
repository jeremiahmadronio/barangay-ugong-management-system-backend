package com.barangay.zshared.dataseeds;

import com.barangay.usermanagement.permission.model.Permission;
import com.barangay.usermanagement.permission.repository.PermissionRepository;
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
        createPermIfNotFound("File Complaint", "BLOTTER", "Allows filing blotter complaints");
        createPermIfNotFound("View Cases", "GLOBAL", "Allows viewing system cases");
        createPermIfNotFound("Manage Mediation", "BLOTTER", "Allows managing mediation processes");
        createPermIfNotFound("Manage Notes", "BLOTTER", "Allows managing case notes and remarks");
        createPermIfNotFound("Settled Case", "BLOTTER", "Allows resolving and settling blotter cases");
        createPermIfNotFound("Dismiss Case", "BLOTTER", "Allows dismissing blotter cases");
        createPermIfNotFound("Edit Case", "BLOTTER", "Allows modifying blotter case details");
        createPermIfNotFound("Escalate to Lupon", "BLOTTER", "Allows escalating blotter cases to Lupon Tagapamayapa");
        createPermIfNotFound("Archive Case", "BLOTTER", "Allows archiving blotter cases");
        createPermIfNotFound("Delete Case", "BLOTTER", "Allows soft deleting blotter cases");
        createPermIfNotFound("Permanent Delete Case", "BLOTTER", "Allows permanently purging blotter cases");
        createPermIfNotFound("Restore Case", "BLOTTER", "Allows restoring archived or deleted blotter cases");

        // GLOBAL / CASE MANAGEMENT PERMISSIONS
        createPermIfNotFound("Create Case Entry", "GLOBAL", "Allows creating new case entries");
        createPermIfNotFound("Archive Cases", "GLOBAL", "Allows archiving inactive or closed cases");
        createPermIfNotFound("Resolve & Finalize Case", "GLOBAL", "Allows resolving and finalizing cases");
        createPermIfNotFound("Manage Case notes", "GLOBAL", "Allows adding and managing case notes");
        createPermIfNotFound("Manage Reports", "GLOBAL", "Allows generating and managing module reports");
        createPermIfNotFound("Issue Referral", "GLOBAL", "Allows issuing external or internal case referrals");
        createPermIfNotFound("Update Case information", "GLOBAL", "Allows modifying case details");

        // LUPON MODULE PERMISSIONS
        createPermIfNotFound("Manage Conciliation", "LUPON", "Allows managing conciliation proceedings");
        createPermIfNotFound("Issue Certified to File Action (CFA)", "LUPON", "Allows issuing certified to file action (CFA)");
        createPermIfNotFound("Manage Case Notes", "LUPON", "Allows managing lupon case notes");

        // VAWC MODULE PERMISSIONS
        createPermIfNotFound("Issue Barangay Protection Order (BPO)", "VAWC", "Allows issuing Barangay Protection Orders");
        createPermIfNotFound("Manage Referral", "VAWC", "Allows managing external or inter-agency case referrals");
        createPermIfNotFound("Issue BPO", "VAWC", "Allows issuing Barangay Protection Orders");
        createPermIfNotFound("Manage Intervention", "VAWC", "Allows managing VAWC social interventions");

        // BCPC MODULE PERMISSIONS
        createPermIfNotFound("Close Case", "BCPC", "Allows closing BCPC cases");
        createPermIfNotFound("Manage Custody & Turn Over", "BCPC", "Allows managing child custody and turn over");
        createPermIfNotFound("Manage Intervention & Monitoring", "BCPC", "Allows managing BCPC child intervention and monitoring");

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
