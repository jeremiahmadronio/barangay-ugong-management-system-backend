package com.barangay.zshared.data_seeds;

import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.user_management.department.model.Department;
import com.barangay.user_management.department.repository.DepartmentRepository;
import com.barangay.zshared.enumerated.OfficerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Data seeder for initial Barangay Officers and Staff.
 * Executes after DepartmentDataSeeder and ResidentDataSeeder (@Order(4)).
 */
@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class OfficerDataSeeder implements CommandLineRunner {

    private final OfficerRepository officerRepository;
    private final PersonRepository personRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (officerRepository.count() > 0) {
            log.info("Officers already seeded. Skipping...");
            return;
        }

        seedOfficers();
    }

    private void seedOfficers() {
        Department luponDept = departmentRepository.findByName("LUPON")
                .orElseGet(() -> departmentRepository.save(new Department(null, "LUPON", "Lupong Tagapamayapa", true)));
        Department vawcDept = departmentRepository.findByName("VAWC")
                .orElseGet(() -> departmentRepository.save(new Department(null, "VAWC", "Violence Against Women and Children", true)));
        Department bcpcDept = departmentRepository.findByName("BCPC")
                .orElseGet(() -> departmentRepository.save(new Department(null, "BCPC", "Barangay Council for Protection of Children", true)));
        Department adminDept = departmentRepository.findByName("ADMIN")
                .orElseGet(() -> departmentRepository.save(new Department(null, "ADMIN", "Administration", true)));

        List<Person> availablePersons = personRepository.findAll();

        if (availablePersons.isEmpty()) {
            log.info("No persons found for seeding officers.");
            return;
        }

        int index = 0;
        // Officer 1: Irene Ramirez
        if (index < availablePersons.size()) {
            Person p1 = availablePersons.get(index++);
            Officer o1 = new Officer(null, "Lupon Secretary", OfficerStatus.ACTIVE, false, 0, p1, luponDept);
            officerRepository.save(o1);
        }

        // Officer 2: Maria Taguiwalo
        if (index < availablePersons.size()) {
            Person p2 = availablePersons.get(index++);
            Officer o2 = new Officer(null, "Lupon Members", OfficerStatus.ACTIVE, false, 0, p2, luponDept);
            officerRepository.save(o2);
        }

        // Officer 3: Ivan Ponce
        if (index < availablePersons.size()) {
            Person p3 = availablePersons.get(index++);
            Officer o3 = new Officer(null, "Pangkat Chairman", OfficerStatus.ACTIVE, false, 0, p3, luponDept);
            officerRepository.save(o3);
        }

        // Officer 4: VAWC Desk Officer
        if (index < availablePersons.size()) {
            Person p4 = availablePersons.get(index++);
            Officer o4 = new Officer(null, "VAWC Lead Officer", OfficerStatus.ACTIVE, false, 0, p4, vawcDept);
            officerRepository.save(o4);
        }

        // Officer 5: BCPC Officer
        if (index < availablePersons.size()) {
            Person p5 = availablePersons.get(index++);
            Officer o5 = new Officer(null, "Child Protection Focal", OfficerStatus.ACTIVE, false, 0, p5, bcpcDept);
            officerRepository.save(o5);
        }

        // Officer 6: Executive Secretary (Global Assignment - linked to ADMIN dept in DB)
        if (index < availablePersons.size()) {
            Person p6 = availablePersons.get(index++);
            Officer o6 = new Officer(null, "Executive Secretary", OfficerStatus.ACTIVE, true, 0, p6, adminDept);
            officerRepository.save(o6);
        }

        log.info("Successfully seeded initial officers.");
    }
}
