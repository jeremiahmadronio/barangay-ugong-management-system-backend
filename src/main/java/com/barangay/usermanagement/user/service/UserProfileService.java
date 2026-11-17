package com.barangay.usermanagement.user.service;

import com.barangay.contentmanagement.model.SystemSetting;
import com.barangay.contentmanagement.repository.SystemSettingRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.usermanagement.department.dto.DepartmentResponse;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.usermanagement.permission.dto.PermissionResponse;
import com.barangay.usermanagement.permission.model.Permission;
import com.barangay.usermanagement.permission.repository.PermissionRepository;
import com.barangay.usermanagement.role.repository.RoleRepository;
import com.barangay.usermanagement.user.dto.UpdatePasswordRequest;
import com.barangay.usermanagement.user.dto.UpdatePersonalInfoRequest;
import com.barangay.usermanagement.user.dto.UserDetailResponse;
import com.barangay.usermanagement.user.dto.UserResponse;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.audit.repository.AuditLogsRepository;
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

import java.time.LocalDate;
import java.time.Period;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserProfileService {


    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PermissionRepository permissionRepository;
    private final AuditLogsRepository auditLogsRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SystemSettingRepository systemSettingRepository;




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




    @Transactional(readOnly = true)
    public boolean isGlobalMfaEnforced() {
        return systemSettingRepository.findById("global_mfa_enabled")
                .map(setting -> "true".equalsIgnoreCase(setting.getSettingValue()))
                .orElse(true);
    }

    public boolean setGlobalMfaEnforced(boolean enabled) {
        SystemSetting setting = systemSettingRepository.findById("global_mfa_enabled")
                .orElse(new SystemSetting("global_mfa_enabled", null));
        setting.setSettingValue(String.valueOf(enabled));
        systemSettingRepository.save(setting);
        return enabled;
    }

    private User findUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        if (Boolean.TRUE.equals(user.getIsArchived())) {
            throw new BusinessRuleException("Cannot modify an archived user. Please restore the account first.");
        }
        return user;
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
}
