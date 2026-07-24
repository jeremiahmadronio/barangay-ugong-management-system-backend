package com.barangay.user_management.user.repository;

import com.barangay.user_management.user.dto.UserCountRawStats;
import com.barangay.user_management.user.dto.UserFilterRequest;
import com.barangay.user_management.user.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Manual queries specifically for the USER entity.
 */
public interface UserRepositoryCustom {

    /**
     * Manual query to get a User and their specific details (Role, Departments).
     */
    Optional<User> findByEmailWithRoleAndDepartments(String email);

    /**
     * Custom aggregation query returning raw count metrics for User Management Dashboard stats.
     */
    UserCountRawStats getRawUserStats(LocalDateTime startOfCurrentMonth);

    /**
     * Retrieves filtered users list for the User Management table based on search, role, accountType, department, and status filters.
     */
    List<User> findFilteredUsers(UserFilterRequest filter);
}
