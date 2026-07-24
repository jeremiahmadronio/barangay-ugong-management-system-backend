package com.barangay.user_management.permission.repository;

import com.barangay.user_management.permission.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    
    Optional<Permission> findByPermissionName(String permissionName);
    
    boolean existsByPermissionName(String permissionName);

    /**
     * Retrieves all permissions ordered by module and permission name for UI display.
     */
    List<Permission> findAllByOrderByModuleAscPermissionNameAsc();
}
