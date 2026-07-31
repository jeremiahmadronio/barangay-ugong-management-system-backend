package com.barangay.usermanagement.department.controller;

import com.barangay.usermanagement.department.dto.DepartmentResponse;
import com.barangay.usermanagement.department.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller exposing endpoints for retrieving Department data for UI select dropdowns.
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
}
