package com.barangay.usermanagement.permission.service;

import com.barangay.usermanagement.permission.dto.PermissionResponse;
import com.barangay.usermanagement.permission.model.Permission;
import com.barangay.usermanagement.permission.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for retrieving permissions for UI display and department-based checkbox filtering.
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    /**
     * Map of department names to allowed permission names matching exact frontend expectations.
     */
    private static final Map<String, List<String>> DEPARTMENT_PERMISSIONS_MAP = new HashMap<>();

    static {
        // VAWC
        DEPARTMENT_PERMISSIONS_MAP.put("VAWC", List.of(
                "File Complaint", "View Cases", "Manage Case Notes", "Settled Case",
                "Dismiss Case", "Issue Barangay Protection Order (BPO)", "Manage Referral",
                "Edit Case", "Archive Case", "Delete Case", "Permanent Delete Case", "Restore Case"
        ));

        // BCPC
        DEPARTMENT_PERMISSIONS_MAP.put("BCPC", List.of(
                "File Complaint", "View Cases", "Dismiss Case", "Close Case", "Settled Case",
                "Edit Case", "Manage Case Notes", "Manage Custody & Turn Over", "Manage Referral",
                "Manage Intervention & Monitoring", "Archive Case", "Delete Case",
                "Permanent Delete Case", "Restore Case"
        ));

        // BLOTTER / BLOTTER MANAGEMENT
        DEPARTMENT_PERMISSIONS_MAP.put("BLOTTER", List.of(
                "File Complaint", "View Cases", "Manage Mediation", "Manage Notes",
                "Settled Case", "Dismiss Case", "Edit Case", "Escalate to Lupon",
                "Re-open Case", "Archive Case", "Delete Case", "Permanent Delete Case", "Restore Case"
        ));

        // CLEARANCE
        DEPARTMENT_PERMISSIONS_MAP.put("CLEARANCE", List.of(
                "Issue Clearance", "View Clearance", "Archive Clearance",
                "Delete Clearance", "Permanent Delete Clearance", "Restore Clearance"
        ));

        // FTJS
        DEPARTMENT_PERMISSIONS_MAP.put("FTJS", List.of(
                "Issue FTJS", "View FTJS", "Re-issue FTJS", "Manage FTJS Notes",
                "Archive FTJS", "Delete FTJS", "Permanent Delete FTJS", "Restore FTJS"
        ));

        // LUPON / LUPONG TAGAPAMAYAPA
        DEPARTMENT_PERMISSIONS_MAP.put("LUPON", List.of(
                "View Cases", "Manage Conciliation", "Issue Certified to File Action (CFA)", "Manage Case Notes",
                "Settled Case", "Dismiss Case", "Archive Case", "Delete Case",
                "Permanent Delete Case", "Restore Case"
        ));

        // KAPITANA / PUNONG BARANGAY (No permissions)
        DEPARTMENT_PERMISSIONS_MAP.put("KAPITANA", Collections.emptyList());
    }

    /**
     * Retrieves all system permissions formatted for checkbox selection.
     */
    @Transactional
    public List<PermissionResponse> getAllPermissions() {
        ensurePermissionsSeeded();
        return permissionRepository.findAllByOrderByModuleAscPermissionNameAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Ensures all department-mapped permissions exist in the database with legitimate IDs.
     */
    @Transactional
    public synchronized void ensurePermissionsSeeded() {
        List<Permission> existing = permissionRepository.findAll();
        Set<String> existingNames = existing.stream()
                .map(p -> p.getPermissionName().trim().toLowerCase())
                .collect(Collectors.toSet());

        List<Permission> toSave = new ArrayList<>();
        DEPARTMENT_PERMISSIONS_MAP.forEach((dept, perms) -> {
            for (String permName : perms) {
                if (!existingNames.contains(permName.trim().toLowerCase())) {
                    Permission p = new Permission();
                    p.setPermissionName(permName.trim());
                    p.setModule(dept);
                    p.setDescription("Allows " + permName.toLowerCase() + " in " + dept);
                    toSave.add(p);
                    existingNames.add(permName.trim().toLowerCase());
                }
            }
        });

        if (!toSave.isEmpty()) {
            permissionRepository.saveAll(toSave);
        }
    }

    /**
     * Retrieves permissions filtered by selected department name or code.
     */
    @Transactional
    public List<PermissionResponse> getPermissionsByDepartment(String departmentName) {
        if (departmentName == null || departmentName.isBlank()) {
            return Collections.emptyList();
        }

        ensurePermissionsSeeded();

        String upperDept = departmentName.trim().toUpperCase();

        // Check if Kapitana or Punong Barangay
        if (upperDept.contains("KAPITANA") || upperDept.contains("PUNONG") || upperDept.contains("OFFICE OF THE BARANGAY CAPTAIN")) {
            return Collections.emptyList();
        }

        // Determine key
        String key = "GLOBAL";
        if (upperDept.contains("VAWC")) key = "VAWC";
        else if (upperDept.contains("BCPC") || upperDept.contains("CHILDREN")) key = "BCPC";
        else if (upperDept.contains("BLOTTER")) key = "BLOTTER";
        else if (upperDept.contains("CLEARANCE") || upperDept.contains("CERTIFICATION")) key = "CLEARANCE";
        else if (upperDept.contains("FTJS") || upperDept.contains("JOB SEEKER")) key = "FTJS";
        else if (upperDept.contains("LUPON") || upperDept.contains("TAGAPAMAYAPA")) key = "LUPON";

        List<String> allowedPermNames = DEPARTMENT_PERMISSIONS_MAP.getOrDefault(key, Collections.emptyList());
        if (allowedPermNames.isEmpty()) {
            return Collections.emptyList();
        }

        // Fetch all permissions and filter by allowed names
        List<Permission> allPerms = permissionRepository.findAll();
        Map<String, Permission> permNameMap = allPerms.stream()
                .collect(Collectors.toMap(p -> p.getPermissionName().trim().toLowerCase(), p -> p, (p1, p2) -> p1));

        List<PermissionResponse> result = new ArrayList<>();
        for (String permName : allowedPermNames) {
            Permission perm = permNameMap.get(permName.trim().toLowerCase());
            if (perm != null) {
                result.add(mapToResponse(perm));
            } else {
                // If not found in DB, return synthetic DTO with unique ID so UI still functions
                result.add(new PermissionResponse((long) (permName.hashCode() & 0x7fffffff), permName, key));
            }
        }

        return result;
    }

    private PermissionResponse mapToResponse(Permission perm) {
        return new PermissionResponse(
                perm.getId(),
                perm.getPermissionName(),
                perm.getModule()
        );
    }
}
