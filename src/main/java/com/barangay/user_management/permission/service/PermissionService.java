package com.barangay.user_management.permission.service;

import com.barangay.user_management.permission.dto.PermissionResponse;
import com.barangay.user_management.permission.model.Permission;
import com.barangay.user_management.permission.repository.PermissionRepository;
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
                "View Cases", "Archive Cases", "Manage Case notes", "Issue Referral",
                "Issue BPO", "Create Case Entry", "Resolve & Finalize Case", "Manage Reports",
                "Update Case information", "Manage Intervention"
        ));

        // BCPC
        DEPARTMENT_PERMISSIONS_MAP.put("BCPC", List.of(
                "Manage Mediation", "View Cases", "Create Case Entry", "Archive Cases",
                "Resolve & Finalize Case", "Manage Case notes", "Manage Reports",
                "Issue Referral", "Update Case information", "Issue BPO", "Manage Intervention"
        ));

        // BLOTTER / BLOTTER MANAGEMENT
        DEPARTMENT_PERMISSIONS_MAP.put("BLOTTER", List.of(
                "View Records", "Manage Mediation", "Manage Lupon Escalation", "View Cases",
                "Create Case Entry", "Archive Cases", "Resolve & Finalize Case",
                "Manage Case notes", "Manage Reports", "Update Case information"
        ));

        // CLEARANCE
        DEPARTMENT_PERMISSIONS_MAP.put("CLEARANCE", List.of(
                "Issue Clearance", "Edit Template", "View Revenue Reports"
        ));

        // FTJS
        DEPARTMENT_PERMISSIONS_MAP.put("FTJS", List.of(
                "Register new Applicant", "Issue ftjs Certificate", "View ftjs Records", "Update Applicant Info"
        ));

        // LUPON / LUPONG TAGAPAMAYAPA
        DEPARTMENT_PERMISSIONS_MAP.put("LUPON", List.of(
                "View Cases", "Create Case Entry", "Archive Cases", "Resolve & Finalize Case",
                "Manage Case notes", "Manage Reports", "Issue Referral", "Update Case information",
                "Manage Conciliation"
        ));

        // KAPITANA / PUNONG BARANGAY (No permissions)
        DEPARTMENT_PERMISSIONS_MAP.put("KAPITANA", Collections.emptyList());
    }

    /**
     * Retrieves all system permissions formatted for checkbox selection.
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAllByOrderByModuleAscPermissionNameAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves permissions filtered by selected department name or code.
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> getPermissionsByDepartment(String departmentName) {
        if (departmentName == null || departmentName.isBlank()) {
            return Collections.emptyList();
        }

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
