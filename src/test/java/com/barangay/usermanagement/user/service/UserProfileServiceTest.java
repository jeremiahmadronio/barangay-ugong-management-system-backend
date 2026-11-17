package com.barangay.usermanagement.user.service;

import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.usermanagement.user.dto.UpdatePasswordRequest;
import com.barangay.usermanagement.user.dto.UpdatePersonalInfoRequest;
import com.barangay.usermanagement.user.dto.UserDetailResponse;
import com.barangay.usermanagement.user.dto.UserResponse;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.exception.BusinessRuleException;
import com.barangay.zshared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    @DisplayName("getUserDetail - Happy path retrieves user profile details")
    void getUserDetail_UserExists_ReturnsDetailResponse() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setUsername("juan_dc");
        user.setSystemEmail("juan@barangay.gov.ph");

        Person person = new Person();
        person.setId(10L);
        person.setFirstName("Juan");
        person.setLastName("Dela Cruz");
        person.setBirthDate(LocalDate.of(1995, 5, 20));
        user.setPerson(person);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        UserDetailResponse response = userProfileService.getUserDetail(userId);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.id());
        assertEquals("juan_dc", response.username());
        assertNotNull(response.personalInfo());
        assertEquals("Juan Dela Cruz", response.personalInfo().fullName());
        verify(userRepository, times(1)).findById(userId);
    }

    @Test
    @DisplayName("getUserDetail - When user not found throws ResourceNotFoundException")
    void getUserDetail_UserNotFound_ThrowsException() {
        // Arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> userProfileService.getUserDetail(userId));
    }

    @Test
    @DisplayName("updatePersonalInfo - Happy path updates mutable personal details")
    void updatePersonalInfo_ValidRequest_UpdatesPersonAndUser() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setSystemEmail("juan@barangay.gov.ph");

        Person person = new Person();
        person.setId(10L);
        person.setFirstName("Juan");
        person.setLastName("Dela Cruz");
        user.setPerson(person);

        UpdatePersonalInfoRequest request = new UpdatePersonalInfoRequest(
                "Johnny",
                null,
                "Dela Cruz",
                null,
                "09123456789",
                "New Address",
                null,
                null,
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(personRepository.save(any(Person.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        UserResponse response = userProfileService.updatePersonalInfo(userId, request);

        // Assert
        assertNotNull(response);
        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(personCaptor.capture());
        Person savedPerson = personCaptor.getValue();
        assertEquals("Johnny", savedPerson.getFirstName());
        assertEquals("New Address", savedPerson.getCompleteAddress());
    }

    @Test
    @DisplayName("updatePassword - Happy path updates hashed password")
    void updatePassword_ValidOldPassword_UpdatesPassword() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setPassword("oldHashedPassword");

        UpdatePasswordRequest request = new UpdatePasswordRequest("OldPassword123!", "NewPassword123!");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword123!", "oldHashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123!")).thenReturn("newHashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        userProfileService.updatePassword(userId, request);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("newHashedPassword", captor.getValue().getPassword());
    }

    @Test
    @DisplayName("updatePassword - When current password does not match throws BusinessRuleException")
    void updatePassword_IncorrectCurrentPassword_ThrowsBusinessRuleException() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);
        user.setPassword("oldHashedPassword");

        UpdatePasswordRequest request = new UpdatePasswordRequest("WrongPassword!", "NewPassword123!");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword!", "oldHashedPassword")).thenReturn(false);

        // Act & Assert
        assertThrows(BusinessRuleException.class, () -> userProfileService.updatePassword(userId, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadProfilePhoto - Happy path saves binary photo bytes")
    void uploadProfilePhoto_ValidImage_SavesPhotoBytes() {
        // Arrange
        Long userId = 1L;
        User user = new User();
        user.setId(userId);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{1, 2, 3, 4}
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        String result = userProfileService.uploadProfilePhoto(userId, file);

        // Assert
        assertNotNull(result);
        assertTrue(result.startsWith("data:image/png;base64,"));
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertNotNull(captor.getValue().getSystemPhoto());
        assertEquals(4, captor.getValue().getSystemPhoto().length);
    }
}
