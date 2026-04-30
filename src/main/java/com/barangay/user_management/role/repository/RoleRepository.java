package com.barangay.user_management.role.repository;

import com.barangay.user_management.role.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Role entity.
 * Handles database operations related to user permissions and access levels.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role,Long> {

    /**
     * Retrieves a role by its unique name.
     * Essential for mapping roles during user authentication and authorization.
     */
    Optional<Role> findByRoleName(String roleName);

    /**
     * Checks if a role name already exists in the database.
     * Used as a validation guard to prevent duplicate role creation.
     */
    boolean existsByRoleName(String roleName);
}
