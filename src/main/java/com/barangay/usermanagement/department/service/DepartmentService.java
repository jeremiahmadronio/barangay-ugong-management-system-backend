package com.barangay.usermanagement.department.service;

import com.barangay.usermanagement.department.dto.DepartmentResponse;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing Department queries and dropdown option lists.
 */
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    /**
     * Retrieves all active department names and IDs formatted for select dropdown options.
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getActiveDepartmentsForDropdown() {
        return departmentRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all departments regardless of active status.
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private DepartmentResponse mapToResponse(Department dept) {
        return new DepartmentResponse(
                dept.getId(),
                dept.getName()
        );
    }
}
