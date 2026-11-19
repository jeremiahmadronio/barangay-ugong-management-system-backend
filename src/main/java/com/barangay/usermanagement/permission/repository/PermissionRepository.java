package com.barangay.usermanagement.permission.repository;

import com.barangay.usermanagement.permission.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    

    boolean existsByPermissionName(String permissionName);

    /**
     * Retrieves all permissions ordered by module and permission name for UI display.
     */
    List<Permission> findAllByOrderByModuleAscPermissionNameAsc();
}
