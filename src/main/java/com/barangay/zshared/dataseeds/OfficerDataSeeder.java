package com.barangay.zshared.dataseeds;

import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.zshared.enumerated.OfficerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data seeder for initial Barangay Officers and Staff.
 * Ensures active officers exist for assignment in formal complaints and records.
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
            log.info("Officers already seeded. Total: {}", officerRepository.count());
            return;
        }

        seedOfficers();
    }

    private void seedOfficers() {
        Department luponDept = departmentRepository.findByName("LUPON")
                .orElseGet(() -> departmentRepository.save(new Department(null, "LUPON", "Lupong Tagapamayapa", true)));
        Department vawcDept = departmentRepository.findByName("VAWC")
                .orElseGet(() -> departmentRepository.save(new Department(null, "VAWC", "Violence Against Women and Children", true)));
        Department adminDept = departmentRepository.findByName("ADMIN")
                .orElseGet(() -> departmentRepository.save(new Department(null, "ADMIN", "Administration", true)));

        createOrGetOfficer("Carlos", "Santos", "Punong Barangay", adminDept, true);
        createOrGetOfficer("Maria", "Cruz", "VAWC Desk Officer", vawcDept, false);
        createOrGetOfficer("Fernando", "Lopez", "Lupon Members", luponDept, false);
        createOrGetOfficer("Elena", "Gomez", "Lupon Secretary", luponDept, false);

        log.info("Successfully seeded 5 active Barangay Officers for case assignment!");
    }

    private void createOrGetOfficer(String firstName, String lastName, String position, Department dept, boolean isGlobal) {
        Person person = new Person();
        person.setFirstName(firstName);
        person.setLastName(lastName);
        person.setContactNumber("09171234567");
        person.setCompleteAddress("Barangay Hall Compound, Ugong, Valenzuela City, Metro Manila");
        person = personRepository.save(person);

        Officer officer = new Officer();
        officer.setPerson(person);
        officer.setPosition(position);
        officer.setDepartment(dept);
        officer.setStatus(OfficerStatus.ACTIVE);
        officer.setIsGlobal(isGlobal);
        officer.setActiveCasesCount(0);

        officerRepository.save(officer);
    }
}
