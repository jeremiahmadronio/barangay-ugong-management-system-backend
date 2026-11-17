package com.barangay.people.officer.service;

import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.zshared.enumerated.OfficerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveOfficerService {

    private final OfficerRepository officerRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getArchivedOfficers(String search) {
        try {
            List<Officer> officers = officerRepository.findAllByStatus(OfficerStatus.ARCHIVED);
            String query = search != null ? search.toLowerCase().trim() : "";

            return officers.stream()
                    .filter(o -> !Boolean.TRUE.equals(o.getIsDeleted()) && !Boolean.TRUE.equals(o.getIsPermanentDeleted()))
                    .filter(o -> {
                        if (query.isEmpty()) return true;
                        Person p = o.getPerson();
                        String fullName = p != null ? (p.getFirstName() + " " + p.getLastName()).toLowerCase() : "";
                        String position = o.getPosition() != null ? o.getPosition().toLowerCase() : "";
                        return fullName.contains(query) || position.contains(query);
                    })
                    .map(o -> {
                        Person p = o.getPerson();
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", o.getId());
                        map.put("employee", p != null ? (p.getFirstName() + " " + p.getLastName()) : "N/A");
                        String deptName = o.getDepartment() != null ? o.getDepartment().getName() : "OVERALL";
                        if ("ADMIN".equalsIgnoreCase(deptName) || "ADMINISTRATION".equalsIgnoreCase(deptName)) {
                            deptName = "OVERALL";
                        }
                        map.put("department", deptName);
                        map.put("position", o.getPosition() != null ? o.getPosition() : "N/A");
                        map.put("status", "ARCHIVED");
                        map.put("statusRemarks", "Account Archived");
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getArchivedOfficers fallback: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void restoreOfficer(Long officerId) {
        Officer officer = officerRepository.findById(officerId).orElse(null);
        if (officer != null) {
            officer.setStatus(OfficerStatus.ACTIVE);
            officer.restoreDelete();
            officerRepository.save(officer);
        }
    }

    @Transactional
    public void softDeleteOfficer(Long officerId) {
        Officer officer = officerRepository.findById(officerId).orElse(null);
        if (officer != null) {
            officer.softDelete();
            officer.setStatus(OfficerStatus.INACTIVE);
            officerRepository.save(officer);
        }
    }

    @Transactional
    public void deleteOfficerPermanent(Long officerId) {
        Officer officer = officerRepository.findById(officerId).orElse(null);
        if (officer != null) {
            officer.permanentDelete();
            officerRepository.save(officer);
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTrashOfficers(String search) {
        try {
            List<Officer> officers = officerRepository.findAll();
            String query = search != null ? search.toLowerCase().trim() : "";

            return officers.stream()
                    .filter(o -> Boolean.TRUE.equals(o.getIsDeleted()) && !Boolean.TRUE.equals(o.getIsPermanentDeleted()))
                    .filter(o -> {
                        if (query.isEmpty()) return true;
                        Person p = o.getPerson();
                        String fullName = p != null ? (p.getFirstName() + " " + p.getLastName()).toLowerCase() : "";
                        String position = o.getPosition() != null ? o.getPosition().toLowerCase() : "";
                        return fullName.contains(query) || position.contains(query);
                    })
                    .map(o -> {
                        Person p = o.getPerson();
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", o.getId());
                        map.put("employee", p != null ? (p.getFirstName() + " " + p.getLastName()) : "N/A");
                        String deptName = o.getDepartment() != null ? o.getDepartment().getName() : "OVERALL";
                        if ("ADMIN".equalsIgnoreCase(deptName) || "ADMINISTRATION".equalsIgnoreCase(deptName)) {
                            deptName = "OVERALL";
                        }
                        map.put("department", deptName);
                        map.put("position", o.getPosition() != null ? o.getPosition() : "N/A");
                        map.put("status", "DELETED");
                        map.put("statusRemarks", "In Recycle Bin");
                        map.put("deletedAt", o.getDeletedAt() != null ? o.getDeletedAt().toString() : "");
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getTrashOfficers fallback: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
