package com.barangay.zshared.data_seeds;


import com.barangay.user_management.department.model.Department;
import com.barangay.user_management.department.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * DepartmentDataSeeder.java
 * Initializer responsible for populating default Barangay departments.
 * Executes second (@Order(2)) to establish department entities for officer mapping.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DepartmentDataSeeder implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedDepartments();
    }

    private void seedDepartments() {
        List<String> departments = List.of(
                "BLOTTER",
                "LUPON",
                "VAWC",
                "BCPC",
                "CLEARANCE",
                "FTJS",
                "ADMIN",
                "KAPITANA"
        );

        departments.forEach(name -> {
            if (departmentRepository.findByName(name).isEmpty()) {
                Department dept = new Department();
                dept.setName(name);
                dept.setIsActive(true);
                departmentRepository.save(dept);
                log.info("Seeded department: {}", name);
            }
        });
    }
}