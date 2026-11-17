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
import com.barangay.zshared.dto.ArchiveStatsDTO;
import com.barangay.zshared.enumerated.MfaType;
import com.barangay.zshared.enumerated.OfficerStatus;
import com.barangay.zshared.enumerated.ResidentStatus;
import com.barangay.zshared.enumerated.UserStatus;
import com.barangay.zshared.exception.BusinessRuleException;
import com.barangay.zshared.exception.DuplicateEntryException;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.notification.service.EmailService;
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
 * Service dedicated to managing System Users (Officers and Staff).
 * Handles account creation, updates, metrics, lock management,
 * and archive operations while excluding administrator accounts.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogsRepository auditLogsRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ResidentRepository residentRepository;
    private final OfficerRepository officerRepository;

    /**
     * Calculates Month-over-Month growth metrics for system users (staff and officers),
     * excluding system root and admin accounts.
     *
     * @return a {@link UserManagementStatsResponse} with trend data.
     */
    @Transactional(readOnly = true)
    public UserManagementStatsResponse getSystemUserStats() {
        LocalDateTime startOfCurrentMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfPrevMonth = startOfCurrentMonth.minusMonths(1);
        LocalDateTime endOfPrevMonth = startOfCurrentMonth.minusNanos(1);

        long currentTotal = userRepository.countTotalSystemUsers(UserStatus.ARCHIVED);
        long previousTotal = userRepository.countTotalSystemUsersBetween(startOfPrevMonth, endOfPrevMonth, UserStatus.ARCHIVED);

        long currentActive = userRepository.countActiveSystemUsers(UserStatus.ARCHIVED, UserStatus.ACTIVE);
        long previousActive = userRepository.countActiveSystemUsersBetween(startOfPrevMonth, endOfPrevMonth, UserStatus.ARCHIVED, UserStatus.ACTIVE);

        long currentLocked = userRepository.countLockedSystemUsers(UserStatus.ARCHIVED, UserStatus.LOCKED);
        long previousLocked = userRepository.countLockedSystemUsersBetween(startOfPrevMonth, endOfPrevMonth, UserStatus.ARCHIVED, UserStatus.LOCKED);

        long currentInactive = userRepository.countInactiveSystemUsers(UserStatus.ARCHIVED, UserStatus.INACTIVE);
        long previousInactive = userRepository.countInactiveSystemUsersBetween(startOfPrevMonth, endOfPrevMonth, UserStatus.ARCHIVED, UserStatus.INACTIVE);

        return new UserManagementStatsResponse(
                calculateMetric(currentTotal, previousTotal),
                calculateMetric(currentActive, previousActive),
                calculateMetric(currentLocked, previousLocked),
                calculateMetric(currentInactive, previousInactive)
        );
    }

    /**
     * Retrieves filtered and searched system users for tabular directory display.
     *
     * @param filter search keyword, department name, and status filter criteria.
     * @return a list of {@link UserTableResponse}.
     */
    @Transactional(readOnly = true)
    public List<UserTableResponse> getFilteredSystemUsersTable(UserFilterRequest filter) {
        String search = (filter != null && filter.search() != null && !filter.search().isBlank()) ? filter.search().trim() : null;
        String departmentName = (filter != null && filter.departmentName() != null && !filter.departmentName().isBlank()) ? filter.departmentName().trim() : null;
        UserStatus status = (filter != null) ? filter.status() : null;

        List<User> users = userRepository.findFilteredSystemUsers(
                search,
                departmentName,
                status,
                UserStatus.ARCHIVED
        );

        return users.stream()
                .map(this::mapToTableResponse)
                .collect(Collectors.toList());
    }

    /**
     * Fetches detailed profile information, linked demographics, and audit activities for a system user.
     *
     * @param userId the ID of the system user.
     * @return a {@link UserDetailResponse} payload.
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getSystemUserDetail(Long userId) {
        User user = findSystemUserById(userId);

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
                "User",
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
     * Provisions a new officer account, associates departments/permissions, hashes credentials,
     * and asynchronously dispatches welcome email with login information.
     *
     * @param request creation request data.
     * @return the created {@link UserResponse}.
     */
    @Transactional
    public UserResponse createSystemUser(UserCreateRequest request) {
        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found with ID: " + request.personId()));

        if (userRepository.existsActiveOrRecentDeletedByPersonId(request.personId())) {
            throw new DuplicateEntryException("User account already exists for this resident/person.");
        }
        if (request.systemEmail() != null && userRepository.existsActiveOrRecentDeletedBySystemEmail(request.systemEmail().trim())) {
            throw new DuplicateEntryException("System email is already registered to an active or recently deleted account: " + request.systemEmail());
        }

        Role officerRole = roleRepository.findByRoleName("OFFICER")
                .orElseThrow(() -> new ResourceNotFoundException("OFFICER role not found in the system."));

        List<Department> departments = departmentRepository.findAllById(request.departmentIds());
        if (departments.isEmpty()) {
            throw new ResourceNotFoundException("No valid departments found for provided IDs.");
        }

        Set<Permission> permissions = new HashSet<>();
        if (request.permissionIds() != null && !request.permissionIds().isEmpty()) {
            permissions.addAll(permissionRepository.findAllById(request.permissionIds()));
        }

        String generatedUsername = generateUniqueUsername(request.systemEmail());
        String tempPassword = generateTemporaryPassword();

        User user = new User();
        user.setPerson(person);
        user.setSystemEmail(request.systemEmail());
        if (request.systemBackupEmail() != null) {
            user.setSystemBackupEmail(request.systemBackupEmail());
        }
        user.setUsername(generatedUsername);
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setRole(officerRole);

        boolean activeFlag = request.isActivated() == null || request.isActivated();
        user.setStatus(activeFlag ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        user.setIsNewAccount(true);
        user.setMfaType(MfaType.EMAIL);
        user.setIsLocked(false);
        user.setFailedAttempts(0);
        user.setIsArchived(false);
        user.setDepartments(new HashSet<>(departments));
        user.setCustomPermissions(permissions);

        User savedUser = userRepository.save(user);

        emailService.sendAccountCredentialsEmail(savedUser.getSystemEmail(), generatedUsername, tempPassword);

        return mapToResponse(savedUser);
    }

    /**
     * Updates an existing system user's email, assigned departments, and active flag.
     *
     * @param userId the ID of the user to update.
     * @param request the update data payload.
     * @return the updated {@link UserResponse}.
     */
    @Transactional
    public UserResponse updateSystemUser(Long userId, UserUpdateRequest request) {
        User user = findSystemUserById(userId);

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

        if (user.getCustomPermissions() == null) {
            user.setCustomPermissions(new HashSet<>());
        } else {
            user.getCustomPermissions().clear();
        }

        if (request.permissionIds() != null && !request.permissionIds().isEmpty()) {
            user.getCustomPermissions().addAll(permissionRepository.findAllById(request.permissionIds()));
        }

        if (request.isActivated() != null) {
            user.setStatus(request.isActivated() ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        }

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    /**
     * Updates a system user's account status (ACTIVE or INACTIVE).
     *
     * @param userId the ID of the user.
     * @param request the status update request.
     * @return the updated {@link UserResponse}.
     */
    @Transactional
    public UserResponse updateSystemUserStatus(Long userId, UserStatusUpdateRequest request) {
        User user = findSystemUserById(userId);

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
     * Locks or unlocks a system user account.
     *
     * @param userId the ID of the user.
     * @param request lock parameters including optional lock duration.
     * @return the updated {@link UserResponse}.
     */
    @Transactional
    public UserResponse toggleSystemUserLock(Long userId, UserLockRequest request) {
        User user = findSystemUserById(userId);

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
     * Soft archives a system user account, marking it inactive and storing remarks.
     *
     * @param userId the ID of the user.
     * @param request remarks detailing reason for archival.
     * @return the updated {@link UserResponse}.
     */
    @Transactional
    public UserResponse archiveSystemUser(Long userId, UserArchiveRequest request) {
        User user = findSystemUserById(userId);

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
     * Restores an archived system user back to active state.
     *
     * @param userId the ID of the user.
     * @return the restored {@link UserResponse}.
     */
    @Transactional
    public UserResponse restoreSystemUser(Long userId) {
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
     * Soft deletes a system user by moving the account into the trash bin.
     *
     * @param userId the ID of the user.
     */
    @Transactional
    public void softDeleteSystemUser(Long userId) {
        User user = findSystemUserById(userId);

        user.softDelete();
        user.setStatus(UserStatus.INACTIVE);
        user.setIsLocked(true);
        userRepository.save(user);
        log.info("System User account ID: {} moved to trash.", userId);
    }

    /**
     * Permanently purges a system user record from the database.
     * Prevents purging administrator accounts.
     *
     * @param userId the ID of the user.
     */
    @Transactional
    public void permanentDeleteSystemUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (Boolean.TRUE.equals(user.getIsSystem()) || (user.getRole() != null && user.getRole().getRoleName().toUpperCase().contains("ADMIN"))) {
            throw new BusinessRuleException("Administrator accounts cannot be deleted from this console.");
        }

        user.permanentDelete();
        user.setStatus(UserStatus.INACTIVE);
        user.setIsLocked(true);
        userRepository.save(user);
        log.info("System User account ID: {} permanently deleted.", userId);
    }

    /**
     * Retrieves overall archive and trash counts for users, residents, and officers.
     *
     * @return an {@link ArchiveStatsDTO} containing count breakdowns.
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
     * Retrieves all archived users matching the optional search keyword.
     *
     * @param search keyword for name or email filter.
     * @return a list of archived user details mapped as key-value pairs.
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
     * Retrieves all soft-deleted users in the trash matching the optional search keyword.
     *
     * @param search keyword for name or email filter.
     * @return a list of soft-deleted user details mapped as key-value pairs.
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

    private User findSystemUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("System user not found with ID: " + userId));

        if (Boolean.TRUE.equals(user.getIsArchived())) {
            throw new BusinessRuleException("Cannot modify an archived user. Please restore the account first.");
        }
        if (Boolean.TRUE.equals(user.getIsSystem())) {
            throw new BusinessRuleException("System root accounts cannot be accessed or modified via this console.");
        }
        if (user.getRole() != null && user.getRole().getRoleName() != null && user.getRole().getRoleName().toUpperCase().contains("ADMIN")) {
            throw new BusinessRuleException("Administrator accounts cannot be modified via this console.");
        }

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

        return new UserTableResponse(
                user.getId(),
                photoBase64,
                fullName,
                firstName,
                lastName,
                user.getUsername(),
                user.getSystemEmail(),
                "User",
                "User",
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
}
