package com.barangay.user_management.user.repository;

import com.barangay.user_management.user.model.User;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * This is for MANUAL queries specifically for the USER entity.
 * We use this when we want to write our own query instead of
 * using the automatic JPA method names.
 */
public interface UserRepositoryCustom {

    /**
     * Manual query to get a User and their specific details (Role, Departments).
     * This is only for the User entity logic.
     */
    Optional<User> findByEmailWithRoleAndDepartments(
            @Param("email") String email
    );
}
