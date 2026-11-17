package com.barangay.usermanagement.user.service;

import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.usermanagement.permission.repository.PermissionRepository;
import com.barangay.usermanagement.role.model.Role;
import com.barangay.usermanagement.role.repository.RoleRepository;
import com.barangay.usermanagement.user.dto.*;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.audit.repository.AuditLogsRepository;
import com.barangay.zshared.dto.ArchiveSettingsDTO;
import com.barangay.zshared.entity.ArchiveSettings;
import com.barangay.zshared.enumerated.UserStatus;
import com.barangay.zshared.exception.BusinessRuleException;
import com.barangay.zshared.exception.DuplicateEntryException;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.notification.service.EmailService;
import com.barangay.zshared.repository.ArchiveSettingsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private AuditLogsRepository auditLogsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private ArchiveSettingsRepository archiveSettingsRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private OfficerRepository officerRepository;

    @InjectMocks
    private SystemAdminService systemAdminService;

    @Test
    @DisplayName("getUserManagementStats - Happy path returns calculated dashboard stats")
    void getUserManagementStats_ValidData_ReturnsStats() {
        // Arrange
        when(userRepository.countTotalUsersInternal(UserStatus.ARCHIVED)).thenReturn(100L);
        when(userRepository.countTotalUsersBetween(any(), any(), eq(UserStatus.ARCHIVED))).thenReturn(80L);
        when(userRepository.countActiveUsersInternal(UserStatus.ARCHIVED, UserStatus.ACTIVE)).thenReturn(90L);
        when(userRepository.countActiveUsersBetween(any(), any(), eq(UserStatus.ARCHIVED), eq(UserStatus.ACTIVE))).thenReturn(70L);
        when(userRepository.countLockedUsersInternal(UserStatus.ARCHIVED)).thenReturn(5L);
        when(userRepository.countLockedUsersBetween(any(), any(), eq(UserStatus.ARCHIVED))).thenReturn(2L);
        when(userRepository.countInactiveUsersInternal(UserStatus.ARCHIVED, UserStatus.INACTIVE)).thenReturn(5L);
        when(userRepository.countInactiveUsersBetween(any(), any(), eq(UserStatus.ARCHIVED), eq(UserStatus.INACTIVE))).thenReturn(8L);

        // Act
        UserManagementStatsResponse response = systemAdminService.getUserManagementStats();

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.totalUsers().currentValue());
        verify(userRepository, times(1)).countTotalUsersInternal(UserStatus.ARCHIVED);
    }

    @Test
    @DisplayName("createUser - Happy path creates officer account, hashes password, saves entity, and sends email")
    void createUser_ValidRequest_Success() {
        // Arrange
        Long personId = 1L;
        UserCreateRequest request = new UserCreateRequest(
                personId,
                "officer@test.com",
                null,
                List.of(1L),
                Collections.emptyList(),
                true
        );

        Person person = new Person();
        person.setId(personId);
        Role role = new Role();
        role.setRoleName("OFFICER");
        Department dept = new Department();
        dept.setId(1L);

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(userRepository.existsActiveOrRecentDeletedByPersonId(personId)).thenReturn(false);
        when(userRepository.existsActiveOrRecentDeletedBySystemEmail("officer@test.com")).thenReturn(false);
        when(roleRepository.findByRoleName("OFFICER")).thenReturn(Optional.of(role));
        when(departmentRepository.findAllById(request.departmentIds())).thenReturn(List.of(dept));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(200L);
            return u;
        });

        // Act
        UserResponse response = systemAdminService.createUser(request);

        // Assert
        assertNotNull(response);
        assertEquals(200L, response.id());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals(UserStatus.ACTIVE, saved.getStatus());
        assertEquals("encodedPassword", saved.getPassword());
        verify(emailService, times(1)).sendAccountCredentialsEmail(eq("officer@test.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("createAdmin - Happy path creates admin account and provisions admin role")
    void createAdmin_ValidRequest_Success() {
        // Arrange
        AdminCreateRequest request = new AdminCreateRequest(
                "Super",
                "Admin",
                "admin@barangay.gov.ph",
                null,
                "ADMIN",
                true
        );

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setRoleName("ADMIN");

        when(userRepository.existsActiveOrRecentDeletedBySystemEmail("admin@barangay.gov.ph")).thenReturn(false);
        when(roleRepository.findByRoleName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(300L);
            return u;
        });

        // Act
        UserResponse response = systemAdminService.createAdmin(request);

        // Assert
        assertNotNull(response);
        assertEquals(300L, response.id());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("ADMIN", saved.getRole().getRoleName());
        verify(emailService, times(1)).sendAccountCredentialsEmail(eq("admin@barangay.gov.ph"), anyString(), anyString());
    }

    @Test
    @DisplayName("updateUser - Attempting to modify system root account throws BusinessRuleException")
    void updateUser_SystemRootUser_ThrowsBusinessRuleException() {
        // Arrange
        Long userId = 99L;
        User systemUser = new User();
        systemUser.setId(userId);
        systemUser.setIsSystem(true);

        UserUpdateRequest request = new UserUpdateRequest("email@test.com", null, List.of(1L), Collections.emptyList(), true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(systemUser));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> systemAdminService.updateUser(userId, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleUserLock - Unlocks account and resets failed login attempts")
    void toggleUserLock_UnlockAccount_ResetsFailedAttempts() {
        // Arrange
        Long userId = 5L;
        User user = new User();
        user.setId(userId);
        user.setStatus(UserStatus.LOCKED);
        user.setIsLocked(true);
        user.setFailedAttempts(5);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        UserResponse response = systemAdminService.toggleUserLock(userId, new UserLockRequest(false, null, null));

        // Assert
        assertNotNull(response);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertFalse(saved.getIsLocked());
        assertEquals(UserStatus.ACTIVE, saved.getStatus());
        assertEquals(0, saved.getFailedAttempts());
    }

    @Test
    @DisplayName("permanentDeleteUser - Attempting to purge a system account throws BusinessRuleException")
    void permanentDeleteUser_SystemAccount_ThrowsBusinessRuleException() {
        // Arrange
        Long userId = 1L;
        User systemUser = new User();
        systemUser.setId(userId);
        systemUser.setIsSystem(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(systemUser));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> systemAdminService.permanentDeleteUser(userId));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSettings - Updates archive retention policies")
    void updateSettings_ValidRequest_SavesAndReturnsSettings() {
        // Arrange
        ArchiveSettings currentSettings = new ArchiveSettings(1L, true, 30, null);
        when(archiveSettingsRepository.findById(1L)).thenReturn(Optional.of(currentSettings));
        when(archiveSettingsRepository.save(any(ArchiveSettings.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        ArchiveSettingsDTO result = systemAdminService.updateSettings(new ArchiveSettingsDTO(false, 60));

        // Assert
        assertNotNull(result);
        assertFalse(result.autoDeleteEnabled());
        assertEquals(60, result.deleteAfterDays());
        verify(archiveSettingsRepository).save(any(ArchiveSettings.class));
    }
}
