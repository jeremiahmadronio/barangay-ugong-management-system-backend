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
import com.barangay.zshared.enumerated.UserStatus;
import com.barangay.zshared.exception.BusinessRuleException;
import com.barangay.zshared.exception.DuplicateEntryException;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.notification.service.EmailService;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

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
    private ResidentRepository residentRepository;

    @Mock
    private OfficerRepository officerRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    @DisplayName("getSystemUserStats - Happy path returns calculated metrics")
    void getSystemUserStats_ValidData_ReturnsStats() {
        // Arrange
        when(userRepository.countTotalSystemUsers(UserStatus.ARCHIVED)).thenReturn(50L);
        when(userRepository.countTotalSystemUsersBetween(any(), any(), eq(UserStatus.ARCHIVED))).thenReturn(40L);
        when(userRepository.countActiveSystemUsers(UserStatus.ARCHIVED, UserStatus.ACTIVE)).thenReturn(45L);
        when(userRepository.countActiveSystemUsersBetween(any(), any(), eq(UserStatus.ARCHIVED), eq(UserStatus.ACTIVE))).thenReturn(35L);
        when(userRepository.countLockedSystemUsers(UserStatus.ARCHIVED, UserStatus.LOCKED)).thenReturn(2L);
        when(userRepository.countLockedSystemUsersBetween(any(), any(), eq(UserStatus.ARCHIVED), eq(UserStatus.LOCKED))).thenReturn(1L);
        when(userRepository.countInactiveSystemUsers(UserStatus.ARCHIVED, UserStatus.INACTIVE)).thenReturn(3L);
        when(userRepository.countInactiveSystemUsersBetween(any(), any(), eq(UserStatus.ARCHIVED), eq(UserStatus.INACTIVE))).thenReturn(4L);

        // Act
        UserManagementStatsResponse response = adminService.getSystemUserStats();

        // Assert
        assertNotNull(response);
        assertNotNull(response.totalUsers());
        assertEquals(50L, response.totalUsers().count());
        verify(userRepository, times(1)).countTotalSystemUsers(UserStatus.ARCHIVED);
    }

    @Test
    @DisplayName("getFilteredSystemUsersTable - Happy path returns mapped list")
    void getFilteredSystemUsersTable_ValidFilter_ReturnsUserList() {
        // Arrange
        User user = new User();
        user.setId(10L);
        user.setUsername("officer1");
        user.setSystemEmail("officer1@barangay.gov.ph");
        user.setStatus(UserStatus.ACTIVE);

        when(userRepository.findFilteredSystemUsers(any(), any(), any(), eq(UserStatus.ARCHIVED)))
                .thenReturn(List.of(user));

        // Act
        List<UserTableResponse> result = adminService.getFilteredSystemUsersTable(new UserFilterRequest("officer", null, null, null));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("officer1", result.get(0).username());
        verify(userRepository, times(1)).findFilteredSystemUsers(any(), any(), any(), eq(UserStatus.ARCHIVED));
    }

    @Test
    @DisplayName("createSystemUser - Happy path creates user, hashes password, saves entity, and sends email")
    void createSystemUser_ValidRequest_Success() {
        // Arrange
        Long personId = 1L;
        UserCreateRequest request = new UserCreateRequest(
                personId,
                "officer@test.com",
                null,
                Set.of(1L),
                Collections.emptySet(),
                true
        );

        Person person = new Person();
        person.setId(personId);
        person.setFirstName("Juan");
        person.setLastName("Dela Cruz");

        Role role = new Role();
        role.setId(2L);
        role.setRoleName("OFFICER");

        Department dept = new Department();
        dept.setId(1L);
        dept.setName("Admin Office");

        when(personRepository.findById(personId)).thenReturn(Optional.of(person));
        when(userRepository.existsActiveOrRecentDeletedByPersonId(personId)).thenReturn(false);
        when(userRepository.existsActiveOrRecentDeletedBySystemEmail("officer@test.com")).thenReturn(false);
        when(roleRepository.findByRoleName("OFFICER")).thenReturn(Optional.of(role));
        when(departmentRepository.findAllById(request.departmentIds())).thenReturn(List.of(dept));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedTempPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(100L);
            return u;
        });

        // Act
        UserResponse response = adminService.createSystemUser(request);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("officer@test.com", response.systemEmail());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        assertEquals("hashedTempPassword", savedUser.getPassword());
        assertTrue(savedUser.getIsNewAccount());
        assertFalse(savedUser.getIsLocked());

        verify(emailService, times(1)).sendAccountCredentialsEmail(eq("officer@test.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("createSystemUser - When Person is not found throws ResourceNotFoundException")
    void createSystemUser_PersonNotFound_ThrowsException() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest(999L, "email@test.com", null, Set.of(1L), Collections.emptySet(), true);
        when(personRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> adminService.createSystemUser(request));
        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendAccountCredentialsEmail(any(), any(), any());
    }

    @Test
    @DisplayName("createSystemUser - When person already has an account throws DuplicateEntryException")
    void createSystemUser_DuplicatePerson_ThrowsDuplicateEntryException() {
        // Arrange
        Long personId = 1L;
        UserCreateRequest request = new UserCreateRequest(personId, "email@test.com", null, Set.of(1L), Collections.emptySet(), true);
        when(personRepository.findById(personId)).thenReturn(Optional.of(new Person()));
        when(userRepository.existsActiveOrRecentDeletedByPersonId(personId)).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateEntryException.class, () -> adminService.createSystemUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSystemUser - When system email already exists throws DuplicateEntryException")
    void createSystemUser_DuplicateEmail_ThrowsDuplicateEntryException() {
        // Arrange
        Long personId = 1L;
        UserCreateRequest request = new UserCreateRequest(personId, "email@test.com", null, Set.of(1L), Collections.emptySet(), true);
        when(personRepository.findById(personId)).thenReturn(Optional.of(new Person()));
        when(userRepository.existsActiveOrRecentDeletedByPersonId(personId)).thenReturn(false);
        when(userRepository.existsActiveOrRecentDeletedBySystemEmail("email@test.com")).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateEntryException.class, () -> adminService.createSystemUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateSystemUser - When modifying email to an already used email throws DuplicateEntryException")
    void updateSystemUser_DuplicateNewEmail_ThrowsDuplicateEntryException() {
        // Arrange
        Long userId = 10L;
        User user = new User();
        user.setId(userId);
        user.setSystemEmail("old@test.com");
        Role role = new Role();
        role.setRoleName("OFFICER");
        user.setRole(role);

        UserUpdateRequest updateRequest = new UserUpdateRequest("new@test.com", null, Set.of(1L), Collections.emptySet(), true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsActiveOrRecentDeletedBySystemEmailExcludingUser("new@test.com", userId)).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateEntryException.class, () -> adminService.updateSystemUser(userId, updateRequest));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("toggleSystemUserLock - Happy path locks user and sets status to LOCKED")
    void toggleSystemUserLock_LockUser_Success() {
        // Arrange
        Long userId = 5L;
        User user = new User();
        user.setId(userId);
        user.setStatus(UserStatus.ACTIVE);
        user.setIsLocked(false);
        Role role = new Role();
        role.setRoleName("OFFICER");
        user.setRole(role);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        UserResponse response = adminService.toggleSystemUserLock(userId, new UserLockRequest(true, 30, "Suspicious activity"));

        // Assert
        assertNotNull(response);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertTrue(saved.getIsLocked());
        assertEquals(UserStatus.LOCKED, saved.getStatus());
        assertEquals("Suspicious activity", saved.getStatusRemarks());
        assertNotNull(saved.getLockUntil());
    }

    @Test
    @DisplayName("archiveSystemUser - Happy path sets archive flag and updates status to ARCHIVED")
    void archiveSystemUser_ValidUser_Success() {
        // Arrange
        Long userId = 7L;
        User user = new User();
        user.setId(userId);
        user.setStatus(UserStatus.ACTIVE);
        Role role = new Role();
        role.setRoleName("OFFICER");
        user.setRole(role);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        UserResponse response = adminService.archiveSystemUser(userId, new UserArchiveRequest("Retired"));

        // Assert
        assertNotNull(response);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertTrue(saved.getIsArchived());
        assertEquals(UserStatus.ARCHIVED, saved.getStatus());
        assertEquals("Retired", saved.getArchiveRemarks());
        assertNotNull(saved.getArchivedAt());
    }

    @Test
    @DisplayName("softDeleteSystemUser - Moves user to trash and updates isDeleted")
    void softDeleteSystemUser_ValidUser_SetsDeletedFlags() {
        // Arrange
        Long userId = 8L;
        User user = new User();
        user.setId(userId);
        user.setStatus(UserStatus.ACTIVE);
        Role role = new Role();
        role.setRoleName("OFFICER");
        user.setRole(role);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        adminService.softDeleteSystemUser(userId);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertTrue(saved.getIsDeleted());
        assertTrue(saved.getIsLocked());
        assertEquals(UserStatus.INACTIVE, saved.getStatus());
        assertNotNull(saved.getDeletedAt());
    }

    @Test
    @DisplayName("permanentDeleteSystemUser - Attempting to purge an admin account throws BusinessRuleException")
    void permanentDeleteSystemUser_AdminAccount_ThrowsBusinessRuleException() {
        // Arrange
        Long userId = 1L;
        User admin = new User();
        admin.setId(userId);
        Role adminRole = new Role();
        adminRole.setRoleName("ADMIN");
        admin.setRole(adminRole);

        when(userRepository.findById(userId)).thenReturn(Optional.of(admin));

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> adminService.permanentDeleteSystemUser(userId));
        verify(userRepository, never()).save(any());
    }
}
