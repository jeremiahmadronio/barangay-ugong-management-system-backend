package com.barangay.usermanagement.permission.controller;

import com.barangay.usermanagement.permission.dto.PermissionResponse;
import com.barangay.usermanagement.permission.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing endpoints for retrieving Permission data for UI checkbox lists.
 */
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * Endpoint to fetch all system permissions or permissions filtered by department name.
     */
    @GetMapping
    public ResponseEntity<List<PermissionResponse>> getPermissions(
            @RequestParam(name = "department", required = false) String department) {

        if (department != null && !department.isBlank()) {
            return ResponseEntity.ok(permissionService.getPermissionsByDepartment(department));
        }

        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    /**
     * Endpoint to fetch permissions for a specific department.
     */
    @GetMapping("/by-department")
    public ResponseEntity<List<PermissionResponse>> getPermissionsByDepartment(
            @RequestParam(name = "departmentName") String departmentName) {
        return ResponseEntity.ok(permissionService.getPermissionsByDepartment(departmentName));
    }
}
