package com.barangay.usermanagement.user.service;

import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemAdminArchiveService {



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
                        map.put("role", u.getRole() != null ? u.getRole().getRoleName() : "USER");
                        String depts = u.getDepartments() != null
                                ? u.getDepartments().stream().map(Department::getName).collect(Collectors.joining(", "))
                                : "N/A";
                        map.put("departments", depts.isEmpty() ? "N/A" : depts);
                        map.put("deletedAt", u.getDeletedAt() != null ? u.getDeletedAt().toString() : "—");
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("getTrashUsers fallback: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
