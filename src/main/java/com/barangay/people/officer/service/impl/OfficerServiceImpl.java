package com.barangay.people.officer.service.impl;

import com.barangay.people.officer.dto.*;
import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.officer.service.OfficerService;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.user_management.department.model.Department;
import com.barangay.user_management.department.repository.DepartmentRepository;
import com.barangay.zshared.enumerated.OfficerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfficerServiceImpl implements OfficerService {

    private final OfficerRepository officerRepository;
    private final PersonRepository personRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    @Transactional(readOnly = true)
    public OfficerStatsResponse getOfficerStats() {
        List<OfficerStatus> activeAndInactive = List.of(OfficerStatus.ACTIVE, OfficerStatus.INACTIVE);
        long totalOfficers = officerRepository.countByStatusIn(activeAndInactive);
        long activeOfficers = officerRepository.countByStatus(OfficerStatus.ACTIVE);
        long inactiveStaff = officerRepository.countByStatus(OfficerStatus.INACTIVE);
        long departmentsCount = departmentRepository.count();

        // Calculate Month-over-Month trend for Total Officers ONLY (ACTIVE + INACTIVE)
        LocalDateTime lastMonthCutoff = LocalDateTime.now().minusMonths(1);
        long previousMonthCount = officerRepository.countByCreatedAtBeforeAndStatusIn(lastMonthCutoff, activeAndInactive);

        String trendText;
        if (previousMonthCount == 0) {
            trendText = totalOfficers > 0 ? "+100% vs last month" : "vs last month";
        } else {
            long diff = totalOfficers - previousMonthCount;
            double percentage = ((double) diff / previousMonthCount) * 100.0;
            if (percentage > 0) {
                trendText = String.format("+%.1f%% vs last month", percentage);
            } else if (percentage < 0) {
                trendText = String.format("%.1f%% vs last month", percentage);
            } else {
                trendText = "0% vs last month";
            }
        }

        return new OfficerStatsResponse(
                totalOfficers,
                trendText,
                activeOfficers,
                inactiveStaff,
                departmentsCount
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfficerTableResponse> getFilteredOfficersTable(OfficerFilterRequest filter) {
        String search = filter != null ? filter.search() : null;
        String dept = filter != null ? filter.departmentName() : null;
        OfficerStatus status = filter != null ? filter.status() : null;

        List<Officer> officers = officerRepository.findFilteredOfficers(search, dept, status);
        return officers.stream()
                .map(this::mapToTableResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OfficerTableResponse createOfficer(OfficerCreateRequest request) {
        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> new IllegalArgumentException("Selected resident/person not found with ID: " + request.personId()));

        Department dept = null;
        boolean isGlobal = Boolean.TRUE.equals(request.isGlobal());

        if (isGlobal || request.departmentId() == null) {
            dept = departmentRepository.findByName("ADMIN")
                    .orElseGet(() -> {
                        Department newDept = new Department();
                        newDept.setName("ADMIN");
                        newDept.setDescription("Administration Department");
                        newDept.setIsActive(true);
                        return departmentRepository.save(newDept);
                    });
            isGlobal = true;
        } else {
            dept = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + request.departmentId()));
        }

        Officer officer = new Officer();
        officer.setPerson(person);
        officer.setDepartment(dept);
        officer.setPosition(request.position());
        officer.setIsGlobal(isGlobal);
        officer.setStatus(request.status() != null ? request.status() : OfficerStatus.ACTIVE);
        officer.setActiveCasesCount(0);

        Officer saved = officerRepository.save(officer);
        return mapToTableResponse(saved);
    }

    @Override
    @Transactional
    public OfficerTableResponse updateOfficer(Long id, OfficerUpdateRequest request) {
        Officer officer = officerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Officer not found with ID: " + id));

        boolean isGlobal = Boolean.TRUE.equals(request.isGlobal());
        if (isGlobal) {
            Department dept = departmentRepository.findByName("ADMIN")
                    .orElseGet(() -> {
                        Department newDept = new Department();
                        newDept.setName("ADMIN");
                        newDept.setDescription("Administration Department");
                        newDept.setIsActive(true);
                        return departmentRepository.save(newDept);
                    });
            officer.setDepartment(dept);
            officer.setIsGlobal(true);
        } else if (request.departmentId() != null) {
            Department dept = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + request.departmentId()));
            officer.setDepartment(dept);
            officer.setIsGlobal(false);
        }

        if (request.position() != null && !request.position().isBlank()) {
            officer.setPosition(request.position());
        }

        if (request.status() != null) {
            officer.setStatus(request.status());
        }

        Officer updated = officerRepository.save(officer);
        return mapToTableResponse(updated);
    }

    @Override
    @Transactional
    public OfficerTableResponse updateOfficerStatus(Long id, OfficerStatusUpdateRequest request) {
        Officer officer = officerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Officer not found with ID: " + id));
        officer.setStatus(request.status());
        Officer updated = officerRepository.save(officer);
        return mapToTableResponse(updated);
    }

    @Override
    @Transactional
    public void deleteOfficer(Long id) {
        if (!officerRepository.existsById(id)) {
            throw new IllegalArgumentException("Officer not found with ID: " + id);
        }
        officerRepository.deleteById(id);
    }

    private OfficerTableResponse mapToTableResponse(Officer officer) {
        Person p = officer.getPerson();
        Department d = officer.getDepartment();

        String fullName = "—";
        String email = "—";
        String photoBase64 = null;

        if (p != null) {
            fullName = buildFullName(p.getFirstName(), p.getMiddleName(), p.getLastName(), p.getSuffix());
            email = p.getEmail() != null ? p.getEmail() : "—";
            if (p.getPhoto() != null && p.getPhoto().length > 0) {
                photoBase64 = Base64.getEncoder().encodeToString(p.getPhoto());
            }
        }

        String deptName = (Boolean.TRUE.equals(officer.getIsGlobal()) || (d != null && ("OVERALL".equalsIgnoreCase(d.getName()) || "ADMIN".equalsIgnoreCase(d.getName()))))
                ? "OVERALL"
                : (d != null ? d.getName() : "OVERALL");
        int cases = officer.getActiveCasesCount() != null ? officer.getActiveCasesCount() : 0;

        return new OfficerTableResponse(
                officer.getId(),
                p != null ? p.getId() : null,
                fullName,
                email,
                d != null ? d.getId() : null,
                deptName,
                officer.getPosition(),
                cases,
                officer.getStatus() != null ? officer.getStatus().name() : "ACTIVE",
                officer.getIsGlobal(),
                photoBase64
        );
    }

    private String buildFullName(String first, String middle, String last, String suffix) {
        StringBuilder sb = new StringBuilder();
        if (first != null) sb.append(first.trim());
        if (middle != null && !middle.isBlank()) sb.append(" ").append(middle.trim());
        if (last != null) sb.append(" ").append(last.trim());
        if (suffix != null && !suffix.isBlank()) sb.append(" ").append(suffix.trim());
        return sb.toString().trim();
    }
}
