package com.barangay.usermanagement.user.service;

import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
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
import com.barangay.zshared.enumerated.MfaType;
import com.barangay.zshared.enumerated.UserStatus;
import com.barangay.zshared.exception.BusinessRuleException;
import com.barangay.zshared.exception.DuplicateEntryException;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.notification.service.EmailService;
import com.barangay.zshared.security.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
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
 * Dedicated service handling operations and metrics for System Users (Officers/Staff only).
 * Excludes Administrator and Root Admin accounts from queries, stats, and modification logic.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemUserService {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogsRepository auditLogsRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Common JPQL WHERE clause fragment ensuring only System Users (non-admins) are targeted.
     */
    private static final String SYSTEM_USER_FILTER = 
            " WHERE (u.isArchived = false OR u.isArchived IS NULL) " +
            " AND (u.status IS NULL OR u.status <> com.barangay.zshared.enumerated.UserStatus.ARCHIVED) " +
            " AND (u.isDeleted = false OR u.isDeleted IS NULL) " +
            " AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL) " +
            " AND (u.isSystem = false OR u.isSystem IS NULL) " +
            " AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin') " +
            " AND (r.roleName IS NOT NULL AND UPPER(r.roleName) NOT LIKE '%ADMIN%') ";

    /**
     * Calculates Month-over-Month dashboard statistics specifically for System Users only.
     */
    @Transactional(readOnly = true)
    public UserManagementStatsResponse getSystemUserStats() {
        LocalDateTime startOfCurrentMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfPrevMonth = startOfCurrentMonth.minusMonths(1);
        LocalDateTime endOfPrevMonth = startOfCurrentMonth.minusNanos(1);

        String fromJoin = "SELECT COUNT(u) FROM User u LEFT JOIN u.role r " + SYSTEM_USER_FILTER;

        String jpqlTotal = fromJoin;
        String jpqlTotalPrev = fromJoin + " AND u.createdAt >= :startOfPrevMonth AND u.createdAt <= :endOfPrevMonth";

        String jpqlActive = fromJoin + " AND u.status = com.barangay.zshared.enumerated.UserStatus.ACTIVE AND (u.isLocked = false OR u.isLocked IS NULL)";
        String jpqlActivePrev = fromJoin + " AND u.status = com.barangay.zshared.enumerated.UserStatus.ACTIVE AND (u.isLocked = false OR u.isLocked IS NULL) AND u.createdAt >= :startOfPrevMonth AND u.createdAt <= :endOfPrevMonth";

        String jpqlLocked = fromJoin + " AND (u.isLocked = true OR u.status = com.barangay.zshared.enumerated.UserStatus.LOCKED)";
        String jpqlLockedPrev = fromJoin + " AND (u.isLocked = true OR u.status = com.barangay.zshared.enumerated.UserStatus.LOCKED) AND u.createdAt >= :startOfPrevMonth AND u.createdAt <= :endOfPrevMonth";

        String jpqlInactive = fromJoin + " AND u.status = com.barangay.zshared.enumerated.UserStatus.INACTIVE";
        String jpqlInactivePrev = fromJoin + " AND u.status = com.barangay.zshared.enumerated.UserStatus.INACTIVE AND u.createdAt >= :startOfPrevMonth AND u.createdAt <= :endOfPrevMonth";

        long currentTotal = entityManager.createQuery(jpqlTotal, Long.class).getSingleResult();
        long previousTotal = entityManager.createQuery(jpqlTotalPrev, Long.class)
                .setParameter("startOfPrevMonth", startOfPrevMonth)
                .setParameter("endOfPrevMonth", endOfPrevMonth)
                .getSingleResult();

        long currentActive = entityManager.createQuery(jpqlActive, Long.class).getSingleResult();
        long previousActive = entityManager.createQuery(jpqlActivePrev, Long.class)
                .setParameter("startOfPrevMonth", startOfPrevMonth)
                .setParameter("endOfPrevMonth", endOfPrevMonth)
                .getSingleResult();

        long currentLocked = entityManager.createQuery(jpqlLocked, Long.class).getSingleResult();
        long previousLocked = entityManager.createQuery(jpqlLockedPrev, Long.class)
                .setParameter("startOfPrevMonth", startOfPrevMonth)
                .setParameter("endOfPrevMonth", endOfPrevMonth)
                .getSingleResult();

        long currentInactive = entityManager.createQuery(jpqlInactive, Long.class).getSingleResult();
        long previousInactive = entityManager.createQuery(jpqlInactivePrev, Long.class)
                .setParameter("startOfPrevMonth", startOfPrevMonth)
                .setParameter("endOfPrevMonth", endOfPrevMonth)
                .getSingleResult();

        return new UserManagementStatsResponse(
                calculateMetric(currentTotal, previousTotal),
                calculateMetric(currentActive, previousActive),
                calculateMetric(currentLocked, previousLocked),
                calculateMetric(currentInactive, previousInactive)
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

    /**
     * Fetches System Users for the management table with search and filtering.
     * Guarantees only accounts with non-admin roles are returned.
     */
    @Transactional(readOnly = true)
    public List<UserTableResponse> getFilteredSystemUsersTable(UserFilterRequest filter) {
        StringBuilder jpql = new StringBuilder("""
                SELECT DISTINCT u FROM User u
                LEFT JOIN FETCH u.person p
                LEFT JOIN FETCH u.role r
                LEFT JOIN FETCH u.departments d
                WHERE (u.isArchived = false OR u.isArchived IS NULL)
                  AND (u.status IS NULL OR u.status <> com.barangay.zshared.enumerated.UserStatus.ARCHIVED)
                  AND (u.isDeleted = false OR u.isDeleted IS NULL)
                  AND (u.isPermanentDeleted = false OR u.isPermanentDeleted IS NULL)
                  AND (u.isSystem = false OR u.isSystem IS NULL)
                  AND (u.username IS NULL OR LOWER(u.username) <> 'superadmin')
                  AND (r.roleName IS NOT NULL AND UPPER(r.roleName) NOT LIKE '%ADMIN%')
                """);

        Map<String, Object> params = new HashMap<>();

        if (filter != null) {
            // Search filter
            if (filter.search() != null && !filter.search().isBlank()) {
                jpql.append("""
                        AND (
                            LOWER(u.username) LIKE LOWER(:search) OR
                            LOWER(u.systemEmail) LIKE LOWER(:search) OR
                            LOWER(p.firstName) LIKE LOWER(:search) OR
                            LOWER(p.lastName) LIKE LOWER(:search) OR
                            LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(:search)
                        )
                        """);
                params.put("search", "%" + filter.search().trim() + "%");
            }

            // Department filter
            if (filter.departmentName() != null && !filter.departmentName().isBlank() && !"ALL".equalsIgnoreCase(filter.departmentName())) {
                jpql.append(" AND EXISTS (SELECT dpt FROM u.departments dpt WHERE LOWER(dpt.name) LIKE LOWER(:deptName)) ");
                params.put("deptName", "%" + filter.departmentName().trim() + "%");
            }

            // Status filter
            if (filter.status() != null) {
                if (filter.status() == UserStatus.LOCKED) {
                    jpql.append(" AND (u.isLocked = true OR u.status = com.barangay.zshared.enumerated.UserStatus.LOCKED) ");
                } else {
                    jpql.append(" AND u.status = :status AND (u.isLocked = false OR u.isLocked IS NULL) ");
                    params.put("status", filter.status());
                }
            }
        }

        jpql.append(" ORDER BY u.createdAt DESC ");

        TypedQuery<User> query = entityManager.createQuery(jpql.toString(), User.class);
        params.forEach(query::setParameter);

        return query.getResultList().stream().map(this::mapToTableResponse).collect(Collectors.toList());
    }

    /**
     * Fetches detailed profile of a specific System User.
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getSystemUserDetail(Long userId) {
        User user = validateAndGetSystemUser(userId);

        // Photo conversion
        String photoBase64 = null;
        byte[] photoBytes = user.getSystemPhoto();
        if ((photoBytes == null || photoBytes.length == 0) && user.getPerson() != null) {
            photoBytes = user.getPerson().getPhoto();
        }
        if (photoBytes != null && photoBytes.length > 0) {
            photoBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(photoBytes);
        }

        // Person Summary
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

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "OFFICER";
        String accountType = "User";

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
     * Creates a new System User (Officer) account.
     */
    @Transactional
    public UserResponse createSystemUser(UserCreateRequest request) {
        Person person = personRepository.findById(request.personId())
                .orElseThrow(() -> new ResourceNotFoundException("Person not found with ID: " + request.personId()));

        if (userRepository.existsActiveOrRecentDeletedByPersonId(request.personId())) {
            throw new DuplicateEntryException("A user account already exists for this person.");
        }
        if (userRepository.existsActiveOrRecentDeletedBySystemEmail(request.systemEmail().trim())) {
            throw new DuplicateEntryException("System email is already registered: " + request.systemEmail());
        }

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

        User user = new User();
        user.setPerson(person);
        user.setSystemEmail(request.systemEmail().trim().toLowerCase());
        user.setSystemBackupEmail(request.systemBackupEmail());
        user.setUsername(generatedUsername);
        user.setPassword(passwordEncoder.encode(tempPassword));
        user.setStatus(Boolean.TRUE.equals(request.isActivated()) ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        user.setIsLocked(false);
        user.setIsNewAccount(true);
        user.setMfaType(MfaType.EMAIL);
        user.setRole(officerRole);
        user.setIsSystem(false);
        user.setDepartments(new HashSet<>(departments));
        user.setCustomPermissions(permissions);

        User savedUser = userRepository.save(user);

        try {
            emailService.sendAccountCredentialsEmail(savedUser.getSystemEmail(), generatedUsername, tempPassword);
        } catch (Exception e) {
            log.warn("Failed to send welcome credentials email: {}", e.getMessage());
        }

        return mapToResponse(savedUser);
    }

    /**
     * Updates an existing System User account.
     */
    @Transactional
    public UserResponse updateSystemUser(Long userId, UserUpdateRequest request) {
        User user = validateAndGetSystemUser(userId);

        if (!user.getSystemEmail().equalsIgnoreCase(request.systemEmail())) {
            if (userRepository.existsActiveOrRecentDeletedBySystemEmailExcludingUser(request.systemEmail().trim(), userId)) {
                throw new DuplicateEntryException("System email is already registered to another account: " + request.systemEmail());
            }
            user.setSystemEmail(request.systemEmail().trim().toLowerCase());
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
     * Updates status (ACTIVE / INACTIVE) for a System User.
     */
    @Transactional
    public UserResponse updateSystemUserStatus(Long userId, UserStatusUpdateRequest request) {
        User user = validateAndGetSystemUser(userId);

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
     * Locks or unlocks a System User account.
     */
    @Transactional
    public UserResponse lockUnlockSystemUser(Long userId, UserLockRequest request) {
        User user = validateAndGetSystemUser(userId);

        boolean willLock = Boolean.TRUE.equals(request.isLocked());
        user.setIsLocked(willLock);

        if (willLock) {
            user.setStatus(UserStatus.LOCKED);
            if (request.lockDurationMinutes() != null && request.lockDurationMinutes() > 0) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(request.lockDurationMinutes()));
            } else {
                user.setLockUntil(null);
            }
        } else {
            user.setStatus(UserStatus.ACTIVE);
            user.setLockUntil(null);
            user.setFailedAttempts(0);
        }

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    /**
     * Archives a System User account (Soft archive).
     */
    @Transactional
    public UserResponse archiveSystemUser(Long userId, UserArchiveRequest request) {
        User user = validateAndGetSystemUser(userId);

        user.setIsArchived(true);
        user.setArchivedAt(LocalDateTime.now());
        user.setArchiveRemarks(request != null ? request.remarks() : null);
        user.setStatus(UserStatus.ARCHIVED);
        user.setIsDeleted(false);
        user.setDeletedAt(null);
        user.setIsPermanentDeleted(false);
        user.setPermanentDeletedAt(null);

        User archivedUser = userRepository.save(user);
        return mapToResponse(archivedUser);
    }

    /**
     * Restores an archived or soft-deleted System User back to ACTIVE status.
     */
    @Transactional
    public UserResponse restoreSystemUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        validateSystemUserRole(user, "Cannot restore an administrator account through System User management.");

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
     * Moves a System User account to the trash (Soft Delete).
     */
    @Transactional
    public void softDeleteSystemUser(Long userId) {
        User user = validateAndGetSystemUser(userId);

        user.softDelete();
        user.setStatus(UserStatus.INACTIVE);
        user.setIsLocked(true);
        userRepository.save(user);
        log.info("System user account ID: {} soft-deleted (moved to trash).", userId);
    }

    /**
     * Flags a System User account as permanently deleted (Audit-safe purge).
     */
    @Transactional
    public void permanentDeleteSystemUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        validateSystemUserRole(user, "Cannot delete administrator or root accounts through System User management.");

        User currentAuth = SecurityUtils.getCurrentUser();
        if (currentAuth != null && currentAuth.getId().equals(userId)) {
            throw new BusinessRuleException("You cannot delete your own active account.");
        }

        user.permanentDelete();
        user.setStatus(UserStatus.INACTIVE);
        user.setIsLocked(true);
        userRepository.save(user);
        log.info("System user account ID: {} flagged as permanently deleted.", userId);
    }

    // ── Validation Helpers ──────────────────────────────────────────────────

    private User validateAndGetSystemUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("System user not found with ID: " + userId));

        validateSystemUserRole(user, "This operation is only valid for System Users (Officers/Staff). Administrator accounts cannot be modified here.");

        if (Boolean.TRUE.equals(user.getIsArchived())) {
            throw new BusinessRuleException("Cannot modify an archived account. Please restore it first.");
        }

        return user;
    }

    private void validateSystemUserRole(User user, String errorMessage) {
        if (Boolean.TRUE.equals(user.getIsSystem())) {
            throw new BusinessRuleException(errorMessage);
        }
        if (user.getRole() != null && user.getRole().getRoleName() != null) {
            String roleUpper = user.getRole().getRoleName().trim().toUpperCase();
            if (roleUpper.contains("ADMIN") || roleUpper.contains("ROOT")) {
                throw new BusinessRuleException(errorMessage);
            }
        }
        if ("superadmin".equalsIgnoreCase(user.getUsername())) {
            throw new BusinessRuleException(errorMessage);
        }
    }

    private boolean isKapitanaDepartment(List<Department> departments) {
        return departments.stream().anyMatch(d ->
                d.getName() != null && d.getName().trim().equalsIgnoreCase("Office of the Barangay Captain")
        );
    }

    private String generateUniqueUsername(String systemEmail) {
        String base = systemEmail.split("@")[0].replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (base.length() < 3) base = "user" + base;
        String candidate = base;
        int counter = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + counter++;
        }
        return candidate;
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%&*";
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
                ? user.getPerson().getFirstName() + " " + (user.getPerson().getLastName() != null ? user.getPerson().getLastName() : "")
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
                fullName.trim(),
                user.getRole() != null ? user.getRole().getId() : null,
                user.getRole() != null ? user.getRole().getRoleName() : "OFFICER",
                deptNames,
                permNames,
                user.getCreatedAt()
        );
    }
}
