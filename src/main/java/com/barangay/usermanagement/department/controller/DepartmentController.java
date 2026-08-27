package com.barangay.usermanagement.department.controller;

import com.barangay.usermanagement.department.dto.DepartmentResponse;
import com.barangay.usermanagement.department.dto.UserDepartmentResponse;
import com.barangay.usermanagement.department.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing endpoints for retrieving Department data.
 */
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * Endpoint to fetch active departments for select dropdown options in frontend.
     */
    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getActiveDepartments() {
        return ResponseEntity.ok(departmentService.getActiveDepartmentsForDropdown());
    }

    /**
     * Endpoint to fetch department IDs and details for a specific user ID via path variable.
     * GET /api/v1/departments/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserDepartmentResponse> getDepartmentsByUserIdPath(@PathVariable Long userId) {
        return ResponseEntity.ok(departmentService.getUserDepartments(userId));
    }

    /**
     * Endpoint to fetch department IDs and details for a specific user ID via query param.
     * GET /api/v1/departments/user-departments?userId=123
     */
    @GetMapping("/user-departments")
    public ResponseEntity<UserDepartmentResponse> getDepartmentsByUserIdParam(@RequestParam Long userId) {
        return ResponseEntity.ok(departmentService.getUserDepartments(userId));
    }
}
