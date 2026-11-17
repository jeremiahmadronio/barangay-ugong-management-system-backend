package com.barangay.usermanagement.user.service;

import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.usermanagement.department.dto.DepartmentResponse;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.usermanagement.permission.dto.PermissionResponse;
import com.barangay.usermanagement.permission.model.Permission;
import com.barangay.usermanagement.permission.repository.PermissionRepository;
import com.barangay.usermanagement.role.model.Role;
import com.barangay.usermanagement.role.repository.RoleRepository;
import com.barangay.usermanagement.user.dto.*;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.audit.repository.AuditLogsRepository;
import com.barangay.zshared.dto.ArchiveSettingsDTO;
import com.barangay.zshared.dto.ArchiveStatsDTO;
import com.barangay.zshared.entity.ArchiveSettings;
import com.barangay.zshared.enumerated.MfaType;
import com.barangay.zshared.enumerated.OfficerStatus;
import com.barangay.zshared.enumerated.ResidentStatus;
import com.barangay.zshared.enumerated.UserStatus;
import com.barangay.zshared.exception.BusinessRuleException;
import com.barangay.zshared.exception.DuplicateEntryException;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.notification.service.EmailService;
import com.barangay.zshared.repository.ArchiveSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing user directory, account provisioning, lifecycle status,
 * and archive retention operations for system administrators.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemAdminService {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogsRepository auditLogsRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ArchiveSettingsRepository archiveSettingsRepository;
    private final ResidentRepository residentRepository;
    private final OfficerRepository officerRepository;

    /**
     * Calculates Month-over-Month dashboard statistics for user management.
     *
     * @return a {@link UserManagementStatsResponse} with trend metrics.
     */
    @Transactional(readOnly = true)
    public UserManagementStatsResponse getUserManagementStats() {
        LocalDateTime startOfCurrentMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfPrevMonth = startOfCurrentMonth.minusMonths(1);
        LocalDateTime endOfPrevMonth = startOfCurrentMonth.minusNanos(1);

        long currentTotal = userRepository.countTotalUsersInternal(UserStatus.ARCHIVED);
        long prevTotal = userRepository.countTotalUsersBetween(startOfPrevMonth, endOfPrevMonth, UserStatus.ARCHIVED);

        long currentActive = userRepository.countActiveUsersInternal(UserStatus.ARCHIVED, UserStatus.ACTIVE);
        long prevActive = userRepository.countActiveUsersBetween(startOfPrevMonth, endOfPrevMonth, UserStatus.ARCHIVED, UserStatus.ACTIVE);

        long currentLocked = userRepository.countLockedUsersInternal(UserStatus.ARCHIVED);
        long prevLocked = userRepository.countLockedUsersBetween(startOfPrevMonth, endOfPrevMonth, UserStatus.ARCHIVED);

        long currentInactive = userRepository.countInactiveUsersInternal(UserStatus.ARCHIVED, UserStatus.INACTIVE);
        long prevInactive = userRepository.countInactiveUsersBetween(startOfPrevMonth, endOfPrevMonth, UserStatus.ARCHIVED, UserStatus.INACTIVE);

        return new UserManagementStatsResponse(
                calculateMetric(currentTotal, prevTotal),
                calculateMetric(currentActive, prevActive),
                calculateMetric(currentLocked, prevLocked),
                calculateMetric(currentInactive, prevInactive)
        );
    }

    /**
     * Retrieves filtered and searched user list for the system directory table.
     *
     * @param filter criteria for keyword, role, department, and status.
     * @return a list of {@link UserTableResponse}.
     */
    @Transactional(readOnly = true)
    public List<UserTableResponse> getFilteredUsersTable(UserFilterRequest filter) {
        String search = (filter != null && filter.search() != null && !filter.search().isBlank()) ? filter.search().trim() : null;
        String roleName = (filter != null && filter.roleName() != null && !filter.roleName().isBlank()) ? filter.roleName().trim() : null;
        String departmentName = (filter != null && filter.departmentName() != null && !filter.departmentName().isBlank()) ? filter.departmentName().trim() : null;
        UserStatus status = (filter != null) ? filter.status() : null;

        List<User> users = userRepository.findFilteredUsers(
                search,
                roleName,
                departmentName,
                status,
                UserStatus.ARCHIVED
        );

        return users.stream().map(this::mapToTableResponse).collect(Collectors.toList());
    }

    /**
     * Checks if a system email is already taken by an active or recently deleted account.
     *
     * @param email system email address to check.
     * @return true if email exists, false otherwise.
     */
    @Transactional(readOnly = true)
    public boolean checkEmailExists(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.existsActiveOrRecentDeletedBySystemEmail(email.trim());
    }

    /**
     * Fetches detailed User Profile for modal/page inspection.
     *
     * @param userId ID of the user.
     * @return a {@link UserDetailResponse} payload.
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        String photoBase64 = null;
        byte[] photoBytes = user.getSystemPhoto();
        if ((photoBytes == null || photoBytes.length == 0) && user.getPerson() != null) {
            photoBytes = user.getPerson().getPhoto();
        }
        if (photoBytes != null && photoBytes.length > 0) {
            photoBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(photoBytes);
        }

        UserDetailResponse.PersonSummaryInfo personInfo = null;
        if (user.getPerson() != null) {
            Person p = user.getPerson();
            String fullName = p.getFirstName() + " " + (p.getLastName() != null ? p.getLastName() : "");
            Integer age = p.getBirthDate() != null ? Period.between(p.getBirthDate(), LocalDate.now()).getYears() : null;

            personInfo = new UserDetailResponse.PersonSummaryInfo(
                    p.getId(),
                    fullName.trim(),
                    p.getFirstName(),
                    p.getMiddleName(),
                    p.getLastName(),
                    p.getSuffix(),
                    p.getGender(),
                    p.getBirthDate(),
                    age,
                    p.getCivilStatus(),
                    p.getContactNumber(),
                    p.getCompleteAddress(),
                    p.getOccupation()
            );
        }

        List<DepartmentResponse> departments = user.getDepartments() != null
                ? user.getDepartments().stream()
                .map(d -> new DepartmentResponse(d.getId(), d.getName()))
                .collect(Collectors.toList())
                : List.of();

        List<PermissionResponse> customPermissions = user.getCustomPermissions() != null
                ? user.getCustomPermissions().stream()
                .map(perm -> new PermissionResponse(perm.getId(), perm.getPermissionName(), perm.getModule()))
                .collect(Collectors.toList())
                : List.of();

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "N/A";
        String accountType = roleName.toUpperCase().contains("ADMIN") ? "Admin" : "User";

        List<UserDetailResponse.UserActivityResponse> recentActivities = auditLogsRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .limit(30)
                .map(logItem -> new UserDetailResponse.UserActivityResponse(
                        logItem.getId(),
                        logItem.getActionTaken(),
                        logItem.getModule() != null ? logItem.getModule().name() : "GENERAL",
                        logItem.getSeverity() != null ? logItem.getSeverity().name() : "INFO",
                        logItem.getReason(),
                        logItem.getIpAddress(),
                        logItem.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new UserDetailResponse(
                user.getId(),
                photoBase64,
                user.getUsername(),
                user.getSystemEmail(),
                user.getSystemBackupEmail(),
                user.getRole() != null ? user.getRole().getId() : null,
                roleName,
                accountType,
                user.getStatus(),
                user.getIsLocked(),
                user.getIsNewAccount(),
                Boolean.TRUE.equals(user.getTotpEnabled()),
                personInfo,
                departments,
                customPermissions,
                user.getLastLoginAt(),
                user.getFailedAttempts(),
                user.getLockUntil(),
                user.getStatusRemarks(),
                recentActivities,
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getArchivedAt(),
                user.getArchiveRemarks()
        );
    }


    /**
     * Provisions a new Officer user account.
     */
    /**
     * Provisions a new officer account with department mappings and temporary password.
     *
     * @param request creation request data.
     * @return the created {@link UserResponse}.
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found with ID: " + request.personId()));

        validateUserUnicity(request.personId(), request.systemEmail());

        Role officerRole = roleRepository.findByRoleName("OFFICER")
                .orElseThrow(() -> new ResourceNotFoundException("OFFICER role not found in the system."));

        List<Department> departments = departmentRepository.findAllById(request.departmentIds());
        if (departments.isEmpty()) {
            throw new ResourceNotFoundException("No valid departments found for provided IDs.");
        }

        boolean isKapitana = isKapitanaDepartment(departments);

        Set<Permission> permissions = new HashSet<>();
        if (!isKapitana && request.permissionIds() != null && !request.permissionIds().isEmpty()) {
            permissions.addAll(permissionRepository.findAllById(request.permissionIds()));
        }

        String generatedUsername = generateUniqueUsername(request.systemEmail());
        String tempPassword = generateTemporaryPassword();

        User user = buildBaseUserEntity(person, request.systemEmail(), request.systemBackupEmail(), generatedUsername, tempPassword, officerRole, request.isActivated());
        user.setDepartments(new HashSet<>(departments));
        user.setCustomPermissions(permissions);

        User savedUser = userRepository.save(user);

        emailService.sendAccountCredentialsEmail(savedUser.getSystemEmail(), generatedUsername, tempPassword);

        return mapToResponse(savedUser);
    }

    /**
     * Provisions a new Administrator account.
     */
    /**
     * Provisions a new administrator account with system roles and credentials.
     *
     * @param request admin creation data payload.
     * @return the created {@link UserResponse}.
     */
    @Transactional
    public UserResponse createAdmin(AdminCreateRequest request) {
        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found with ID: " + request.personId()));

        validateUserUnicity(request.personId(), request.systemEmail());

        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseGet(() -> roleRepository.findByRoleName("SYSTEM_ADMIN")
                        .orElseThrow(() -> new ResourceNotFoundException("Neither ADMIN nor SYSTEM_ADMIN role was found in the system.")));

        String generatedUsername = generateUniqueUsername(request.systemEmail());
        String tempPassword = generateTemporaryPassword();

        User adminUser = buildBaseUserEntity(person, request.systemEmail(), request.systemBackupEmail(), generatedUsername, tempPassword, adminRole, request.isActivated());

        List<Department> allDepts = departmentRepository.findAll();
        Set<Department> nonAdminDepts = allDepts.stream()
                .filter(d -> !d.getName().trim().equalsIgnoreCase("ADMIN") && !d.getName().trim().equalsIgnoreCase("SYSTEM_ADMIN"))
                .collect(Collectors.toSet());
        adminUser.setDepartments(nonAdminDepts);

        User savedAdmin = userRepository.save(adminUser);

        emailService.sendAccountCredentialsEmail(savedAdmin.getSystemEmail(), generatedUsername, tempPassword);

        return mapToResponse(savedAdmin);
    }

    /**
     * Updates an existing Officer account.
     *
     * @param userId the ID of the officer to update.
     * @param request update payload with emails and departments.
     * @return the updated {@link UserResponse}.
     */
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = findUserById(userId);
        validateNonSystemUser(user, "System root accounts cannot be modified via standard user update.");

        if (!user.getSystemEmail().equalsIgnoreCase(request.systemEmail())) {
            if (userRepository.existsActiveOrRecentDeletedBySystemEmailExcludingUser(request.systemEmail().trim(), userId)) {
                throw new DuplicateEntryException("System email is already registered to another active or recently deleted account: " + request.systemEmail());
            }
            user.setSystemEmail(request.systemEmail());
        }

        if (request.systemBackupEmail() != null) {
            user.setSystemBackupEmail(request.systemBackupEmail());
        }

        List<Department> departments = departmentRepository.findAllById(request.departmentIds());
        if (departments.isEmpty()) {
            throw new ResourceNotFoundException("No valid departments found for provided IDs.");
        }
        if (user.getDepartments() == null) {
            user.setDepartments(new HashSet<>());
        } else {
            user.getDepartments().clear();
        }
        user.getDepartments().addAll(departments);

        boolean isKapitana = isKapitanaDepartment(departments);

        if (user.getCustomPermissions() == null) {
            user.setCustomPermissions(new HashSet<>());
        } else {
            user.getCustomPermissions().clear();
        }

        if (!isKapitana && request.permissionIds() != null && !request.permissionIds().isEmpty()) {
            user.getCustomPermissions().addAll(permissionRepository.findAllById(request.permissionIds()));
        }

        if (request.isActivated() != null) {
            user.setStatus(request.isActivated() ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        }

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    /**
     * Updates an existing Administrator account.
     *
     * @param userId the ID of the administrator account.
     * @param request update payload with emails and status.
     * @return the updated {@link UserResponse}.
     */
    @Transactional
    public UserResponse updateAdmin(Long userId, AdminUpdateRequest request) {
        User adminUser = findUserById(userId);
        validateNonSystemUser(adminUser, "System root accounts cannot be modified via standard admin update.");

        if (!adminUser.getSystemEmail().equalsIgnoreCase(request.systemEmail())) {
            if (userRepository.existsActiveOrRecentDeletedBySystemEmailExcludingUser(request.systemEmail().trim(), userId)) {
                throw new DuplicateEntryException("System email is already registered to another active or recently deleted account: " + request.systemEmail());
            }
            adminUser.setSystemEmail(request.systemEmail());
        }

        if (request.systemBackupEmail() != null) {
            adminUser.setSystemBackupEmail(request.systemBackupEmail());
        }

        if (request.isActivated() != null) {
            adminUser.setStatus(request.isActivated() ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        }

        User savedAdmin = userRepository.save(adminUser);
        return mapToResponse(savedAdmin);
    }

    /**
     * Updates a user's status (ACTIVE / INACTIVE).
     *
     * @param userId the ID of the user.
     * @param request payload containing new status and remarks.
     * @return the updated {@link UserResponse}.
     */
    @Transactional
    public UserResponse updateUserStatus(Long userId, UserStatusUpdateRequest request) {
        User user = findUserById(userId);
        validateNonSystemUser(user, "System root accounts cannot have their status modified.");

        user.setStatus(request.status());
        if (request.remarks() != null && !request.remarks().isBlank()) {
            user.setStatusRemarks(request.remarks());
        }

        if (request.status() == UserStatus.ACTIVE) {
            user.setIsLocked(false);
            user.setLockUntil(null);
            user.setFailedAttempts(0);
        }

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    /**
     * Locks or unlocks a user account.
     *
     * @param userId the ID of the user.
     * @param request lock request parameters.
     * @return the updated {@link UserResponse}.
     */
    @Transactional
    public UserResponse toggleUserLock(Long userId, UserLockRequest request) {
        User user = findUserById(userId);
        validateNonSystemUser(user, "System root accounts cannot be locked or unlocked.");

        boolean shouldLock = Boolean.TRUE.equals(request.isLocked());

        if (shouldLock) {
            user.setIsLocked(true);
            user.setStatus(UserStatus.LOCKED);
            if (request.lockDurationMinutes() != null && request.lockDurationMinutes() > 0) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(request.lockDurationMinutes()));
            } else {
                user.setLockUntil(null);
            }
            if (request.reason() != null && !request.reason().isBlank()) {
                user.setStatusRemarks(request.reason());
            }
        } else {
            user.setIsLocked(false);
            user.setLockUntil(null);
            user.setFailedAttempts(0);
            user.setStatus(UserStatus.ACTIVE);
            user.setStatusRemarks("Account unlocked by administrator.");
        }

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    /**
     * Archives a user account.
     *
     * @param userId the ID of the user.
     * @param request optional archive remarks.
     * @return the updated {@link UserResponse}.
     */
    @Transactional
    public UserResponse archiveUser(Long userId, UserArchiveRequest request) {
        User user = findUserById(userId);
        validateNonSystemUser(user, "System root accounts cannot be archived.");

        user.setIsArchived(true);
        user.setArchivedAt(LocalDateTime.now());
        if (request != null && request.remarks() != null && !request.remarks().isBlank()) {
            user.setArchiveRemarks(request.remarks().trim());
        }
        user.setStatus(UserStatus.ARCHIVED);
        user.setIsDeleted(false);
        user.setDeletedAt(null);
        user.setIsPermanentDeleted(false);
        user.setPermanentDeletedAt(null);

        User archivedUser = userRepository.save(user);
        return mapToResponse(archivedUser);
    }

    /**
     * Restores an archived or soft-deleted user account back to active status.
     *
     * @param userId the ID of the user.
     * @return the restored {@link UserResponse}.
     */
    @Transactional
    public UserResponse restoreUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setIsArchived(false);
        user.setArchivedAt(null);
        user.setArchiveRemarks(null);
        user.setIsDeleted(false);
        user.setDeletedAt(null);
        user.setIsPermanentDeleted(false);
        user.setPermanentDeletedAt(null);
        user.setStatus(UserStatus.ACTIVE);
        user.setIsLocked(false);
        user.setLockUntil(null);
        user.setFailedAttempts(0);

        User restoredUser = userRepository.save(user);
        return mapToResponse(restoredUser);
    }

    /**
     * Moves a user account to trash (Soft Delete).
     *
     * @param userId the ID of the user to soft delete.
     */
    @Transactional
    public void softDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        validateNonSystemUser(user, "System root accounts cannot be deleted from the system.");

        user.softDelete();
        user.setStatus(UserStatus.INACTIVE);
        user.setIsLocked(true);
        userRepository.save(user);
        log.info("User account with ID: {} soft-deleted (moved to trash).", userId);
    }

    /**
     * Permanently purges a user account from the system.
     * Prevents purging system root accounts.
     *
     * @param userId the ID of the user.
     */
    @Transactional
    public void permanentDeleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        validateNonSystemUser(user, "System root accounts cannot be deleted from the system.");

        user.permanentDelete();
        user.setStatus(UserStatus.INACTIVE);
        user.setIsLocked(true);
        userRepository.save(user);
        log.info("User account with ID: {} flagged as permanently deleted.", userId);
    }

    /**
     * Retrieves archive retention settings (auto-delete flag and retention days).
     *
     * @return an {@link ArchiveSettingsDTO}.
     */
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

    /**
     * Updates archive retention configuration parameters.
     *
     * @param request retention settings payload.
     * @return the saved {@link ArchiveSettingsDTO}.
     */
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

    /**
     * Retrieves counts for archived and trash records across users, residents, and officers.
     *
     * @return an {@link ArchiveStatsDTO} summary.
     */
    @Transactional(readOnly = true)
    public ArchiveStatsDTO getArchiveStats() {
        long archivedResidents = 0;
        try {
            archivedResidents = residentRepository.countByStatus(ResidentStatus.ARCHIVED);
        } catch (Exception e) {
            log.warn("count archived residents fallback: {}", e.getMessage());
        }

        long archivedOfficers = 0;
        try {
            archivedOfficers = officerRepository.countByStatus(OfficerStatus.ARCHIVED);
        } catch (Exception e) {
            log.warn("count archived officers fallback: {}", e.getMessage());
        }

        long archivedUsers = 0;
        try {
            archivedUsers = userRepository.countArchivedUsers();
        } catch (Exception e) {
            log.warn("count archived users fallback: {}", e.getMessage());
        }

        long totalArchived = archivedResidents + archivedOfficers + archivedUsers;

        long deletedResidents = 0;
        try {
            deletedResidents = residentRepository.countTrashResidents();
        } catch (Exception e) {
            log.warn("count deleted residents fallback: {}", e.getMessage());
        }

        long deletedOfficers = 0;
        try {
            deletedOfficers = officerRepository.countByStatus(OfficerStatus.INACTIVE);
        } catch (Exception e) {
            log.warn("count deleted officers fallback: {}", e.getMessage());
        }

        long deletedUsers = 0;
        try {
            deletedUsers = userRepository.countTrashUsers();
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

    /**
     * Lists archived users matching search query keyword.
     *
     * @param search keyword for name or email filter.
     * @return a list of archived users mapped as key-value pairs.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getArchivedUsers(String search) {
        try {
            List<User> users = userRepository.findArchivedUsersWithDetails(UserStatus.ARCHIVED);
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

    /**
     * Lists soft-deleted users in trash matching search query keyword.
     *
     * @param search keyword for name or email filter.
     * @return a list of soft-deleted users mapped as key-value pairs.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTrashUsers(String search) {
        try {
            List<User> users = userRepository.findTrashUsersWithDetails();
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

    // =========================================================================
    // 6. PRIVATE HELPER METHODS
    // =========================================================================

    private User findUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        if (Boolean.TRUE.equals(user.getIsArchived())) {
            throw new BusinessRuleException("Cannot modify an archived user. Please restore the account first.");
        }
        return user;
    }

    private void validateNonSystemUser(User user, String errorMessage) {
        if (Boolean.TRUE.equals(user.getIsSystem())) {
            throw new BusinessRuleException(errorMessage);
        }
    }

    private void validateUserUnicity(Long personId, String systemEmail) {
        if (personId != null && userRepository.existsActiveOrRecentDeletedByPersonId(personId)) {
            throw new DuplicateEntryException("User account already exists for this resident/person.");
        }
        if (systemEmail != null && userRepository.existsActiveOrRecentDeletedBySystemEmail(systemEmail.trim())) {
            throw new DuplicateEntryException("System email is already registered to an active or recently deleted account: " + systemEmail);
        }
    }

    private User buildBaseUserEntity(Person person, String systemEmail, String backupEmail, String username, String tempPassword, Role role, Boolean isActivated) {
        User user = new User();
        user.setPerson(person);
        user.setSystemEmail(systemEmail);
        if (backupEmail != null) {
            user.setSystemBackupEmail(backupEmail);
        }
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setRole(role);

        boolean activeFlag = isActivated == null || isActivated;
        user.setStatus(activeFlag ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        user.setIsNewAccount(true);
        user.setMfaType(MfaType.EMAIL);
        user.setIsLocked(false);
        user.setFailedAttempts(0);
        user.setIsArchived(false);

        return user;
    }

    private String generateUniqueUsername(String systemEmail) {
        String base = systemEmail.split("@")[0].toLowerCase().replaceAll("[^a-z0-9._]", "");
        if (base.isBlank()) base = "user";

        String username = base;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + counter;
            counter++;
        }
        return username;
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private UserTableResponse mapToTableResponse(User user) {
        String photoBase64 = null;
        byte[] photoBytes = user.getSystemPhoto();
        if ((photoBytes == null || photoBytes.length == 0) && user.getPerson() != null) {
            photoBytes = user.getPerson().getPhoto();
        }
        if (photoBytes != null && photoBytes.length > 0) {
            photoBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(photoBytes);
        }

        String fullName = user.getPerson() != null
                ? user.getPerson().getFirstName() + " " + user.getPerson().getLastName()
                : "N/A";

        String firstName = user.getPerson() != null ? user.getPerson().getFirstName() : "";
        String lastName = user.getPerson() != null ? user.getPerson().getLastName() : "";

        List<String> depts = user.getDepartments() != null
                ? user.getDepartments().stream().map(Department::getName).collect(Collectors.toList())
                : List.of();

        String rawRole = user.getRole() != null ? user.getRole().getRoleName() : "USER";
        String roleName = rawRole.toUpperCase().contains("ADMIN") ? "Admin" : "User";
        String accountType = rawRole.toUpperCase().contains("ADMIN") ? "Admin" : "User";

        return new UserTableResponse(
                user.getId(),
                photoBase64,
                fullName,
                firstName,
                lastName,
                user.getUsername(),
                user.getSystemEmail(),
                roleName,
                accountType,
                depts,
                user.getStatus(),
                user.getIsLocked(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    private UserResponse mapToResponse(User user) {
        String fullName = user.getPerson() != null
                ? user.getPerson().getFirstName() + " " + user.getPerson().getLastName()
                : "N/A";

        Set<String> deptNames = user.getDepartments() != null
                ? user.getDepartments().stream().map(Department::getName).collect(Collectors.toSet())
                : Set.of();

        Set<String> permNames = user.getCustomPermissions() != null
                ? user.getCustomPermissions().stream().map(Permission::getPermissionName).collect(Collectors.toSet())
                : Set.of();

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getSystemEmail(),
                user.getStatus(),
                user.getIsNewAccount(),
                user.getPerson() != null ? user.getPerson().getId() : null,
                fullName,
                user.getRole() != null ? user.getRole().getId() : null,
                user.getRole() != null ? user.getRole().getRoleName() : null,
                deptNames,
                permNames,
                user.getCreatedAt()
        );
    }

    private StatMetric calculateMetric(long currentCount, long previousCount) {
        double change;
        if (currentCount == 0) {
            change = 0.0;
        } else if (previousCount == 0) {
            change = 100.0;
        } else {
            change = ((double) (currentCount - previousCount) / (double) currentCount) * 100.0;
        }

        String direction;
        if (change > 0) {
            direction = "UP";
        } else if (change < 0) {
            direction = "DOWN";
        } else {
            direction = "NEUTRAL";
        }

        double roundedChange = Math.round(change * 10.0) / 10.0;
        String formattedSign = roundedChange > 0 ? "+" : "";
        String text = formattedSign + roundedChange + "% vs last month";

        return new StatMetric(currentCount, roundedChange, direction, text);
    }

    private boolean isKapitanaDepartment(List<Department> departments) {
        if (departments == null || departments.isEmpty()) {
            return false;
        }
        return departments.stream().anyMatch(d -> {
            String name = d.getName() != null ? d.getName().trim().toUpperCase() : "";
            return name.contains("KAPITANA") || name.contains("PUNONG") || name.contains("BARANGAY CAPTAIN");
        });
    }
}
