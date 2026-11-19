package com.barangay.usermanagement.department.service;

import com.barangay.usermanagement.department.dto.DepartmentResponse;
import com.barangay.usermanagement.department.dto.UserDepartmentResponse;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
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
    private final UserRepository userRepository;

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
     * Retrieves all departments assigned to a specific User by User ID.
     */
    @Transactional(readOnly = true)
    public UserDepartmentResponse getUserDepartments(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        List<DepartmentResponse> deptResponses = user.getDepartments()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        List<Long> deptIds = deptResponses.stream()
                .map(DepartmentResponse::id)
                .collect(Collectors.toList());

        return new UserDepartmentResponse(userId, deptIds, deptResponses);
    }



    private DepartmentResponse mapToResponse(Department dept) {
        return new DepartmentResponse(
                dept.getId(),
                dept.getName()
        );
    }
}
