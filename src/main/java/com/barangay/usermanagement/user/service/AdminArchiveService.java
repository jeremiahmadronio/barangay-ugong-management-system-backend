package com.barangay.usermanagement.user.service;

import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.dto.ArchiveStatsDTO;
import com.barangay.zshared.enumerated.OfficerStatus;
import com.barangay.zshared.enumerated.ResidentStatus;
import com.barangay.zshared.enumerated.UserStatus;
import com.barangay.zshared.exception.BusinessRuleException;
import com.barangay.zshared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dedicated Archive Service for Barangay Admin.
 * Targets System Users only (Officers/Staff), excluding Admin and Root Admin accounts
 * from archived/deleted user lists and metric calculations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminArchiveService {

    private final UserRepository userRepository;
    private final ResidentRepository residentRepository;
    private final OfficerRepository officerRepository;
    private final ArchiveService archiveService;

    /**
     * Checks if a User entity is a regular System User (non-admin, non-system root).
     */
    private boolean isSystemUser(User u) {
        if (Boolean.TRUE.equals(u.getIsSystem())) {
            return false;
        }
        if (u.getUsername() != null && "superadmin".equalsIgnoreCase(u.getUsername().trim())) {
            return false;
        }
        if (u.getRole() != null && u.getRole().getRoleName() != null) {
            String roleUpper = u.getRole().getRoleName().trim().toUpperCase();
            if (roleUpper.contains("ADMIN") || roleUpper.contains("ROOT")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calculates Archive & Trash vault statistics for Barangay Admin.
     * User metrics count only System Users (excluding Admins).
     */
    @Transactional(readOnly = true)
    public ArchiveStatsDTO getArchiveStats() {
        // --- Archived Counts ---
        long archivedResidents = 0;
        try {
            archivedResidents = residentRepository.findAll().stream()
                    .filter(r -> r.getStatus() == ResidentStatus.ARCHIVED
                            && !Boolean.TRUE.equals(r.getIsDeleted()) && !Boolean.TRUE.equals(r.getIsPermanentDeleted()))
                    .count();
        } catch (Exception e) {
            log.warn("count archived residents fallback: {}", e.getMessage());
        }

        long archivedOfficers = 0;
        try {
            archivedOfficers = officerRepository.findAll().stream()
                    .filter(o -> o.getStatus() == OfficerStatus.ARCHIVED
                            && !Boolean.TRUE.equals(o.getIsDeleted()) && !Boolean.TRUE.equals(o.getIsPermanentDeleted()))
                    .count();
        } catch (Exception e) {
            log.warn("count archived officers fallback: {}", e.getMessage());
        }

        long archivedUsers = 0;
        try {
            archivedUsers = userRepository.findAll().stream()
                    .filter(u -> (u.getStatus() == UserStatus.ARCHIVED || Boolean.TRUE.equals(u.getIsArchived()))
                            && !Boolean.TRUE.equals(u.getIsDeleted())
                            && !Boolean.TRUE.equals(u.getIsPermanentDeleted())
                            && isSystemUser(u))
                    .count();
        } catch (Exception e) {
            log.warn("count archived users fallback: {}", e.getMessage());
        }

        long totalArchived = archivedResidents + archivedOfficers + archivedUsers;

        // --- Trash / Soft-Deleted Counts ---
        long deletedResidents = 0;
        try {
            deletedResidents = residentRepository.findAll().stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsDeleted()) && !Boolean.TRUE.equals(r.getIsPermanentDeleted()))
                    .count();
        } catch (Exception e) {
            log.warn("count deleted residents fallback: {}", e.getMessage());
        }

        long deletedOfficers = 0;
        try {
            deletedOfficers = officerRepository.findAll().stream()
                    .filter(o -> Boolean.TRUE.equals(o.getIsDeleted()) && !Boolean.TRUE.equals(o.getIsPermanentDeleted()))
                    .count();
        } catch (Exception e) {
            log.warn("count deleted officers fallback: {}", e.getMessage());
        }

        long deletedUsers = 0;
        try {
            deletedUsers = userRepository.findAll().stream()
                    .filter(u -> Boolean.TRUE.equals(u.getIsDeleted())
                            && !Boolean.TRUE.equals(u.getIsPermanentDeleted())
                            && isSystemUser(u))
                    .count();
        } catch (Exception e) {
            log.warn("count deleted users fallback: {}", e.getMessage());
        }

        long totalDeleted = deletedResidents + deletedOfficers + deletedUsers;

        return new ArchiveStatsDTO(
                totalArchived,
                archivedResidents,
                archivedOfficers,
                archivedUsers,
                totalDeleted,
                deletedResidents,
                deletedOfficers,
                deletedUsers
        );
    }

    // ── Archived System Users ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getArchivedUsers(String search) {
        try {
            List<User> users = userRepository.findAllByStatus(UserStatus.ARCHIVED);
            String query = search != null ? search.toLowerCase().trim() : "";

            return users.stream()
                    .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()) && !Boolean.TRUE.equals(u.getIsPermanentDeleted()))
                    .filter(this::isSystemUser)
                    .filter(u -> {
                        if (query.isEmpty()) return true;
                        Person p = u.getPerson();
                        String fullName = p != null ? (p.getFirstName() + " " + p.getLastName()).toLowerCase() : (u.getUsername() != null ? u.getUsername().toLowerCase() : "");
                        String email = u.getSystemEmail() != null ? u.getSystemEmail().toLowerCase() : "";
                        return fullName.contains(query) || email.contains(query);
                    })
                    .map(u -> {
                        Person p = u.getPerson();
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", u.getId());
                        map.put("fullName", p != null ? (p.getFirstName() + " " + p.getLastName()) : (u.getUsername() != null ? u.getUsername() : "User"));
                        map.put("email", u.getSystemEmail() != null ? u.getSystemEmail() : "N/A");
                        map.put("role", "OFFICER");
                        String depts = u.getDepartments() != null
                                ? u.getDepartments().stream().map(Department::getName).collect(Collectors.joining(", "))
                                : "N/A";
                        map.put("departments", depts.isEmpty() ? "N/A" : depts);
                        map.put("status", "ARCHIVED");
                        map.put("lastLogin", u.getLastLoginAt() != null ? u.getLastLoginAt().toString() : "—");
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getArchivedUsers fallback: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTrashUsers(String search) {
        try {
            List<User> users = userRepository.findAll();
            String query = search != null ? search.toLowerCase().trim() : "";

            return users.stream()
                    .filter(u -> Boolean.TRUE.equals(u.getIsDeleted()) && !Boolean.TRUE.equals(u.getIsPermanentDeleted()))
                    .filter(this::isSystemUser)
                    .filter(u -> {
                        if (query.isEmpty()) return true;
                        Person p = u.getPerson();
                        String fullName = p != null ? (p.getFirstName() + " " + p.getLastName()).toLowerCase() : (u.getUsername() != null ? u.getUsername().toLowerCase() : "");
                        String email = u.getSystemEmail() != null ? u.getSystemEmail().toLowerCase() : "";
                        return fullName.contains(query) || email.contains(query);
                    })
                    .map(u -> {
                        Person p = u.getPerson();
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", u.getId());
                        map.put("fullName", p != null ? (p.getFirstName() + " " + p.getLastName()) : (u.getUsername() != null ? u.getUsername() : "User"));
                        map.put("email", u.getSystemEmail() != null ? u.getSystemEmail() : "N/A");
                        map.put("role", "OFFICER");
                        String depts = u.getDepartments() != null
                                ? u.getDepartments().stream().map(Department::getName).collect(Collectors.joining(", "))
                                : "N/A";
                        map.put("departments", depts.isEmpty() ? "N/A" : depts);
                        map.put("status", "DELETED");
                        map.put("deletedAt", u.getDeletedAt() != null ? u.getDeletedAt().toString() : "");
                        map.put("lastLogin", u.getLastLoginAt() != null ? u.getLastLoginAt().toString() : "—");
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getTrashUsers fallback: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void restoreUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        if (!isSystemUser(user)) {
            throw new BusinessRuleException("Administrator accounts cannot be restored via this service.");
        }
        archiveService.restoreUser(userId);
    }

    @Transactional
    public void softDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        if (!isSystemUser(user)) {
            throw new BusinessRuleException("Administrator accounts cannot be moved to trash via this service.");
        }
        archiveService.softDeleteUser(userId);
    }

    @Transactional
    public void deleteUserPermanent(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        if (!isSystemUser(user)) {
            throw new BusinessRuleException("Administrator accounts cannot be permanently deleted via this service.");
        }
        archiveService.deleteUserPermanent(userId);
    }

    // ── Delegates for Residents & Officers ──────────────────────────────────

    public List<Map<String, Object>> getArchivedResidents(String search) {
        return archiveService.getArchivedResidents(search);
    }

    public void restoreResident(Long residentId) {
        archiveService.restoreResident(residentId);
    }

    public void softDeleteResident(Long residentId) {
        archiveService.softDeleteResident(residentId);
    }

    public void deleteResidentPermanent(Long residentId) {
        archiveService.deleteResidentPermanent(residentId);
    }

    public List<Map<String, Object>> getArchivedOfficers(String search) {
        return archiveService.getArchivedOfficers(search);
    }

    public void restoreOfficer(Long officerId) {
        archiveService.restoreOfficer(officerId);
    }

    public void softDeleteOfficer(Long officerId) {
        archiveService.softDeleteOfficer(officerId);
    }

    public void deleteOfficerPermanent(Long officerId) {
        archiveService.deleteOfficerPermanent(officerId);
    }

    public List<Map<String, Object>> getTrashResidents(String search) {
        return archiveService.getTrashResidents(search);
    }

    public List<Map<String, Object>> getTrashOfficers(String search) {
        return archiveService.getTrashOfficers(search);
    }
}
