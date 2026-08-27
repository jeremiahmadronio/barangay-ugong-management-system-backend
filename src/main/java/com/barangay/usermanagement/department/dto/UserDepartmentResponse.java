package com.barangay.usermanagement.department.dto;

import java.util.List;

/**
 * DTO containing department IDs and details assigned to a specific User.
 */
public record UserDepartmentResponse(
        Long userId,
        List<Long> departmentIds,
        List<DepartmentResponse> departments
) {
}
