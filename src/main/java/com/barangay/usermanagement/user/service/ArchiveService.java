package com.barangay.usermanagement.user.service;

import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.dto.ArchiveSettingsDTO;
import com.barangay.zshared.dto.ArchiveStatsDTO;
import com.barangay.zshared.entity.ArchiveSettings;
import com.barangay.zshared.enumerated.OfficerStatus;
import com.barangay.zshared.enumerated.ResidentStatus;
import com.barangay.zshared.enumerated.UserStatus;
import com.barangay.zshared.repository.ArchiveSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveService {

    private final ArchiveSettingsRepository archiveSettingsRepository;
    private final ResidentRepository residentRepository;
    private final OfficerRepository officerRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public ArchiveSettingsDTO getSettings() {
        try {
            ArchiveSettings settings = archiveSettingsRepository.findById(1L)
                    .orElseGet(() -> new ArchiveSettings(1L, true, 30, LocalDateTime.now()));
            return new ArchiveSettingsDTO(settings.getAutoDeleteEnabled(), settings.getDeleteAfterDays());
        } catch (Exception e) {
            log.warn("ArchiveSettings getSettings fallback: {}", e.getMessage());
            return new ArchiveSettingsDTO(true, 30);
        }
    }

    @Transactional
    public ArchiveSettingsDTO updateSettings(ArchiveSettingsDTO request) {
        try {
            ArchiveSettings settings = archiveSettingsRepository.findById(1L)
                    .orElseGet(() -> new ArchiveSettings(1L, true, 30, LocalDateTime.now()));

            if (request.autoDeleteEnabled() != null) {
                settings.setAutoDeleteEnabled(request.autoDeleteEnabled());
            }
            if (request.deleteAfterDays() != null) {
                settings.setDeleteAfterDays(request.deleteAfterDays());
            }
            settings.setUpdatedAt(LocalDateTime.now());

            ArchiveSettings saved = archiveSettingsRepository.save(settings);
            return new ArchiveSettingsDTO(saved.getAutoDeleteEnabled(), saved.getDeleteAfterDays());
        } catch (Exception e) {
            log.warn("ArchiveSettings updateSettings fallback: {}", e.getMessage());
            return new ArchiveSettingsDTO(
                    request.autoDeleteEnabled() != null ? request.autoDeleteEnabled() : true,
                    request.deleteAfterDays() != null ? request.deleteAfterDays() : 30
            );
        }
    }

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
                            && !Boolean.TRUE.equals(u.getIsDeleted()) && !Boolean.TRUE.equals(u.getIsPermanentDeleted()))
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
                    .filter(u -> Boolean.TRUE.equals(u.getIsDeleted()) && !Boolean.TRUE.equals(u.getIsPermanentDeleted()))
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

    // --- Archived Residents ---
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getArchivedResidents(String search) {
        try {
            List<Resident> residents = residentRepository.findAllByStatus(ResidentStatus.ARCHIVED);
            String query = search != null ? search.toLowerCase().trim() : "";

            return residents.stream()
                    .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()) && !Boolean.TRUE.equals(r.getIsPermanentDeleted()))
                    .filter(r -> {
                        if (query.isEmpty()) return true;
                        Person p = r.getPerson();
                        String fullName = p != null ? (p.getFirstName() + " " + p.getLastName()).toLowerCase() : "";
                        String barangayId = r.getBarangayIdNumber() != null ? r.getBarangayIdNumber().toLowerCase() : "";
                        return fullName.contains(query) || barangayId.contains(query);
                    })
                    .map(r -> {
                        Person p = r.getPerson();
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", r.getResidentId());
                        map.put("fullName", p != null ? (p.getFirstName() + " " + (p.getMiddleName() != null ? p.getMiddleName() + " " : "") + p.getLastName()) : "N/A");
                        map.put("barangayId", r.getBarangayIdNumber() != null ? r.getBarangayIdNumber() : "N/A");
                        map.put("contact", p != null && p.getContactNumber() != null ? p.getContactNumber() : "N/A");
                        map.put("status", "ARCHIVED");
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getArchivedResidents fallback: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Transactional
    public void restoreResident(Long residentId) {
        Resident resident = residentRepository.findById(residentId).orElse(null);
        if (resident != null) {
            resident.setStatus(ResidentStatus.ACTIVE);
            resident.restoreDelete();
            residentRepository.save(resident);
        }
    }

    @Transactional
    public void softDeleteResident(Long residentId) {
        Resident resident = residentRepository.findById(residentId).orElse(null);
        if (resident != null) {
            resident.softDelete();
            resident.setStatus(ResidentStatus.INACTIVE);
            residentRepository.save(resident);
        }
    }

    @Transactional
    public void deleteResidentPermanent(Long residentId) {
        Resident resident = residentRepository.findById(residentId).orElse(null);
        if (resident != null) {
            resident.permanentDelete();
            residentRepository.save(resident);
        }
    }

    // --- Archived Users ---
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getArchivedUsers(String search) {
        try {
            List<User> users = userRepository.findAllByStatus(UserStatus.ARCHIVED);
            String query = search != null ? search.toLowerCase().trim() : "";

            return users.stream()
                    .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()) && !Boolean.TRUE.equals(u.getIsPermanentDeleted()))
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
                        map.put("role", u.getRole() != null ? u.getRole().getRoleName() : "N/A");
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

    @Transactional
    public void restoreUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setStatus(UserStatus.ACTIVE);
            user.setIsArchived(false);
            user.setArchivedAt(null);
            user.setArchiveRemarks(null);
            user.setIsLocked(false);
            user.setLockUntil(null);
            user.restoreDelete();
            userRepository.save(user);
        }
    }

    @Transactional
    public void softDeleteUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.softDelete();
            user.setStatus(UserStatus.INACTIVE);
            user.setIsLocked(true);
            userRepository.save(user);
        }
    }

    @Transactional
    public void deleteUserPermanent(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.permanentDelete();
            user.setStatus(UserStatus.INACTIVE);
            user.setIsLocked(true);
            userRepository.save(user);
        }
    }

    // --- Archived Officers ---
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

    // --- Trash / Soft-Deleted Residents ---
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTrashResidents(String search) {
        try {
            List<Resident> residents = residentRepository.findAll();
            String query = search != null ? search.toLowerCase().trim() : "";

            return residents.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getIsDeleted()) && !Boolean.TRUE.equals(r.getIsPermanentDeleted()))
                    .filter(r -> {
                        if (query.isEmpty()) return true;
                        Person p = r.getPerson();
                        String fullName = p != null ? (p.getFirstName() + " " + p.getLastName()).toLowerCase() : "";
                        String barangayId = r.getBarangayIdNumber() != null ? r.getBarangayIdNumber().toLowerCase() : "";
                        return fullName.contains(query) || barangayId.contains(query);
                    })
                    .map(r -> {
                        Person p = r.getPerson();
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", r.getResidentId());
                        map.put("fullName", p != null ? (p.getFirstName() + " " + (p.getMiddleName() != null ? p.getMiddleName() + " " : "") + p.getLastName()) : "N/A");
                        map.put("barangayId", r.getBarangayIdNumber() != null ? r.getBarangayIdNumber() : "N/A");
                        map.put("contact", p != null && p.getContactNumber() != null ? p.getContactNumber() : "N/A");
                        map.put("status", "DELETED");
                        map.put("deletedAt", r.getDeletedAt() != null ? r.getDeletedAt().toString() : "");
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getTrashResidents fallback: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // --- Trash / Soft-Deleted Users ---
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTrashUsers(String search) {
        try {
            List<User> users = userRepository.findAll();
            String query = search != null ? search.toLowerCase().trim() : "";

            return users.stream()
                    .filter(u -> Boolean.TRUE.equals(u.getIsDeleted()) && !Boolean.TRUE.equals(u.getIsPermanentDeleted()))
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
                        map.put("role", u.getRole() != null ? u.getRole().getRoleName() : "N/A");
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

    // --- Trash / Soft-Deleted Officers ---
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
