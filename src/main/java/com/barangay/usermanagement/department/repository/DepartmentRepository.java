package com.barangay.usermanagement.department.repository;

import com.barangay.usermanagement.department.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    Optional<Department> findByName(String name);

    /**
     * Retrieves all active departments ordered alphabetically by name for dropdowns.
     */
    List<Department> findByIsActiveTrueOrderByNameAsc();

    Optional<Department> findByNameIgnoreCase(String name);
}
