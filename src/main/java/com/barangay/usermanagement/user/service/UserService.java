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
import com.barangay.zshared.enumerated.MfaType;
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
import org.springframework.web.multipart.MultipartFile;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service handling User and Admin account provisioning, updating, profile viewing,
 * User Management stats, status updates, account locking/unlocking, archiving/restoring, and filtered Table data queries.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PermissionRepository permissionRepository;
    private final com.barangay.zshared.audit.repository.AuditLogsRepository auditLogsRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Checks if a system email is already taken by an active or recently deleted (non-permanently-deleted) user account.
     * Returns true if taken, false if available (or if former accounts with this email are permanently deleted).
     */
    @Transactional(readOnly = true)
    public boolean checkEmailExists(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userRepository.existsActiveOrRecentDeletedBySystemEmail(email.trim());
    }

    /**
     * Fetches detailed User Profile for viewing.
     */
    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Base64 Photo conversion
        String photoBase64 = null;
        byte[] photoBytes = user.getSystemPhoto();
        if ((photoBytes == null || photoBytes.length == 0) && user.getPerson() != null) {
            photoBytes = user.getPerson().getPhoto();
        }
        if (photoBytes != null && photoBytes.length > 0) {
            photoBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(photoBytes);
        }

        // Person Summary Info Mapping
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

        // Departments Mapping
        List<DepartmentResponse> departments = user.getDepartments() != null
                ? user.getDepartments().stream()
                .map(d -> new DepartmentResponse(d.getId(), d.getName()))
                .collect(Collectors.toList())
                : List.of();

        // Custom Permissions Mapping
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
     * Updates an existing Officer user account (email, backup email, departments, permissions, status).
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
     * Updates an existing Administrator account (email, backup email, status).
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
     * Partially updates personal info (username, contact number, and optionally first/last name).
     * Only fields that actually changed are updated and saved to the database.
     * System email is strictly read-only and immutable.
     */
    @Transactional
    public UserResponse updatePersonalInfo(Long userId, UpdatePersonalInfoRequest request) {
        User user = findUserById(userId);
        boolean userModified = false;
        boolean personModified = false;

        // 1. Partial update: Username (only if changed and valid)
        if (request.username() != null && !request.username().isBlank()) {
            String trimmedUsername = request.username().trim();
            if (!trimmedUsername.equalsIgnoreCase(user.getUsername())) {
                if (userRepository.existsByUsernameExcludingUser(trimmedUsername, user.getId())) {
                    throw new DuplicateEntryException("Username '" + trimmedUsername + "' is already in use by another account.");
                }
                user.setUsername(trimmedUsername);
                userModified = true;
                log.info("Updated username for user ID: {} to '{}'", userId, trimmedUsername);
            }
        }

        // 2. Partial update: Person details (contactNumber, firstName, lastName)
        Person person = user.getPerson();
        if (person != null) {
            // Contact Number (only if changed)
            if (request.contactNumber() != null) {
                String trimmedContact = request.contactNumber().trim();
                String currentContact = person.getContactNumber() != null ? person.getContactNumber().trim() : "";
                if (!trimmedContact.equals(currentContact)) {
                    person.setContactNumber(trimmedContact);
                    personModified = true;
                    log.info("Updated contact number for person ID: {} (user ID: {}) to '{}'", person.getId(), userId, trimmedContact);
                }
            }

            // First Name (only if changed)
            if (request.firstName() != null && !request.firstName().isBlank()) {
                String trimmedFirstName = request.firstName().trim();
                String currentFirstName = person.getFirstName() != null ? person.getFirstName().trim() : "";
                if (!trimmedFirstName.equalsIgnoreCase(currentFirstName)) {
                    person.setFirstName(trimmedFirstName);
                    personModified = true;
                    log.info("Updated first name for person ID: {} (user ID: {}) to '{}'", person.getId(), userId, trimmedFirstName);
                }
            }

            // Last Name (only if changed)
            if (request.lastName() != null && !request.lastName().isBlank()) {
                String trimmedLastName = request.lastName().trim();
                String currentLastName = person.getLastName() != null ? person.getLastName().trim() : "";
                if (!trimmedLastName.equalsIgnoreCase(currentLastName)) {
                    person.setLastName(trimmedLastName);
                    personModified = true;
                    log.info("Updated last name for person ID: {} (user ID: {}) to '{}'", person.getId(), userId, trimmedLastName);
                }
            }

            if (personModified) {
                personRepository.save(person);
            }
        }

        if (userModified) {
            user = userRepository.save(user);
        }

        return mapToResponse(user);
    }

    /**
     * Updates the profile photo for a user account (and linked person entity if present).
     * Validates file size (max 5MB) and mime type (images only).
     *
     * @param userId the ID of the user whose photo is to be updated
     * @param file   the uploaded image file
     * @return Base64 data URL representation of the saved photo
     */
    @Transactional
    public String updateProfilePhoto(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Please select an image file to upload.");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessRuleException("Profile photo size must not exceed 5MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessRuleException("Only valid image formats (JPEG, PNG, WebP) are accepted.");
        }

        User user = findUserById(userId);
        try {
            byte[] bytes = file.getBytes();
            user.setSystemPhoto(bytes);
            userRepository.save(user);

            if (user.getPerson() != null) {
                Person person = user.getPerson();
                person.setPhoto(bytes);
                personRepository.save(person);
            }

            log.info("Profile photo successfully updated for user ID: {}", userId);
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (java.io.IOException e) {
            log.error("Failed to read uploaded photo for user ID: {}", userId, e);
            throw new BusinessRuleException("Failed to process uploaded photo: " + e.getMessage());
        }
    }

    /**
     * Retrieves the profile photo binary data for a user.
     */
    @Transactional(readOnly = true)
    public byte[] getUserPhotoBytes(Long userId) {
        User user = findUserById(userId);
        byte[] bytes = user.getSystemPhoto();
        if ((bytes == null || bytes.length == 0) && user.getPerson() != null) {
            bytes = user.getPerson().getPhoto();
        }
        return bytes;
    }

    /**
     * Changes password for an authenticated user with verification and validation.
     * Enforces current password verification, new password confirmation, complexity,
     * prevents reusing current password, and securely hashes with BCrypt.
     */
    @Transactional
    public void changePassword(Long userId, UpdatePasswordRequest request) {
        User user = findUserById(userId);

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessRuleException("New password and confirmation password do not match.");
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BusinessRuleException("The current password you entered is incorrect.");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BusinessRuleException("New password cannot be the same as your current password.");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Password successfully updated and BCrypt-hashed for user ID: {}", userId);
    }

    /**
     * Updates a user's status (ACTIVE / INACTIVE).
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
     */
    @Transactional
    public UserResponse lockUnlockUser(Long userId, UserLockRequest request) {
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
     * Clears all archive, soft-delete, and permanent-delete flags.
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
     * Moves a user account to the trash (Soft Delete).
     * Disables account login and excludes from active directory without breaking audit or case history foreign keys.
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
     * Permanently flags a user account as deleted (Audit-safe purge).
     * Prevents deletion of System Root Admin accounts.
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
     * Default removal from User Management table performs soft delete.
     */
    @Transactional
    public void deleteUser(Long userId) {
        softDeleteUser(userId);
    }

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

    @Transactional(readOnly = true)
    public List<UserTableResponse> getFilteredUsersTable(UserFilterRequest filter) {
        List<User> users = userRepository.findFilteredUsers(filter);
        return users.stream().map(this::mapToTableResponse).collect(Collectors.toList());
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

    @Transactional(readOnly = true)
    public UserManagementStatsResponse getUserManagementStats() {
        LocalDateTime startOfCurrentMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        UserCountRawStats rawStats = userRepository.getRawUserStats(startOfCurrentMonth);

        return new UserManagementStatsResponse(
                calculateMetric(rawStats.currentTotal(), rawStats.previousTotal()),
                calculateMetric(rawStats.currentActive(), rawStats.previousActive()),
                calculateMetric(rawStats.currentLocked(), rawStats.previousLocked()),
                calculateMetric(rawStats.currentInactive(), rawStats.previousInactive())
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

        // Automatically assign all non-admin departments to Admin accounts
        List<Department> allDepts = departmentRepository.findAll();
        Set<Department> nonAdminDepts = allDepts.stream()
                .filter(d -> !d.getName().trim().equalsIgnoreCase("ADMIN") && !d.getName().trim().equalsIgnoreCase("SYSTEM_ADMIN"))
                .collect(Collectors.toSet());
        adminUser.setDepartments(nonAdminDepts);

        User savedAdmin = userRepository.save(adminUser);

        emailService.sendAccountCredentialsEmail(savedAdmin.getSystemEmail(), generatedUsername, tempPassword);

        return mapToResponse(savedAdmin);
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
