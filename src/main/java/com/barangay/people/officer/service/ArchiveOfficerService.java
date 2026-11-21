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

/**
 * Service managing archived and soft-deleted Barangay Officers.
 * Handles retrieval of archived records, lifecycle restorations, soft deletion,
 * permanent purging, and trash inspection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveOfficerService {

    private final OfficerRepository officerRepository;

    /**
     * Retrieve all officers marked with ARCHIVED status matching an optional search term.
     *
     * @param search keyword matching employee name or position
     * @return list of mapped officer archive summaries
     */
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

    /**
     * Restore an archived or soft-deleted officer back to ACTIVE status.
     *
     * @param officerId primary key identifier of the officer
     */
    @Transactional
    public void restoreOfficer(Long officerId) {
        Officer officer = officerRepository.findById(officerId).orElse(null);
        if (officer != null) {
            officer.setStatus(OfficerStatus.ACTIVE);
            officer.restoreDelete();
            officerRepository.save(officer);
        }
    }

    /**
     * Move an officer to the trash bin (soft delete) and set status to INACTIVE.
     *
     * @param officerId primary key identifier of the officer
     */
    @Transactional
    public void softDeleteOfficer(Long officerId) {
        Officer officer = officerRepository.findById(officerId).orElse(null);
        if (officer != null) {
            officer.softDelete();
            officer.setStatus(OfficerStatus.INACTIVE);
            officerRepository.save(officer);
        }
    }

    /**
     * Mark an officer as permanently deleted.
     *
     * @param officerId primary key identifier of the officer
     */
    @Transactional
    public void deleteOfficerPermanent(Long officerId) {
        Officer officer = officerRepository.findById(officerId).orElse(null);
        if (officer != null) {
            officer.permanentDelete();
            officerRepository.save(officer);
        }
    }

    /**
     * Retrieve all soft-deleted officers currently stored in the recycle bin.
     *
     * @param search keyword matching employee name or position
     * @return list of mapped trash officer summaries
     */
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
