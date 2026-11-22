package com.barangay.contentmanagement.service;

import com.barangay.contentmanagement.dto.OfficialDto;
import com.barangay.contentmanagement.model.Official;
import com.barangay.contentmanagement.repository.OfficialRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * BDD Unit tests for {@link OfficialService}.
 * Verifies directory listings, registration, profile updates, and archiving.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OfficialService BDD Tests")
class OfficialServiceTest {

    @Mock
    private OfficialRepository officialRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private OfficialService officialService;

    @Nested
    @DisplayName("Directory Retrieval Tests")
    class DirectoryRetrievalTests {

        @Test
        @DisplayName("givenActiveOfficials_whenGetPublicOfficials_thenReturnList")
        void givenActiveOfficials_whenGetPublicOfficials_thenReturnList() {
            // Given
            Official official = new Official();
            official.setId(1L);
            official.setName("Hon. Juan Dela Cruz");
            official.setPosition("Punong Barangay");
            official.setType("CAPTAIN");

            given(officialRepository.findAllActive()).willReturn(List.of(official));

            // When
            List<Official> result = officialService.getPublicOfficials();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Hon. Juan Dela Cruz");
            then(officialRepository).should().findAllActive();
        }
    }

    @Nested
    @DisplayName("Registration and Profile Updates")
    class RegistrationAndUpdateTests {

        @Test
        @DisplayName("givenValidOfficialDtoWithImage_whenCreateOfficial_thenSaveAndReturnOfficial")
        void givenValidOfficialDtoWithImage_whenCreateOfficial_thenSaveAndReturnOfficial() {
            // Given
            OfficialDto dto = new OfficialDto();
            dto.setName("Hon. Maria Santos");
            dto.setPosition("Barangay Kagawad");
            dto.setType("KAGAWAD");

            MockMultipartFile file = new MockMultipartFile("image", "kagawad.png", "image/png", "fake-img".getBytes());
            dto.setImage(file);

            given(fileStorageService.storeFile(file)).willReturn("/uploads/kagawad.png");
            given(officialRepository.save(any(Official.class))).willAnswer(invocation -> {
                Official saved = invocation.getArgument(0);
                saved.setId(10L);
                return saved;
            });

            // When
            Official result = officialService.createOfficial(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getImageUrl()).isEqualTo("/uploads/kagawad.png");
            then(officialRepository).should().save(any(Official.class));
        }

        @Test
        @DisplayName("givenNonExistentId_whenUpdateOfficial_thenThrowRuntimeException")
        void givenNonExistentId_whenUpdateOfficial_thenThrowRuntimeException() {
            // Given
            OfficialDto dto = new OfficialDto();
            given(officialRepository.findById(999L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> officialService.updateOfficial(999L, dto, false))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Official not found with id: 999");
        }
    }

    @Nested
    @DisplayName("Lifecycle Management Tests")
    class LifecycleManagementTests {

        @Test
        @DisplayName("givenExistingOfficial_whenArchiveOfficial_thenSetArchivedTrue")
        void givenExistingOfficial_whenArchiveOfficial_thenSetArchivedTrue() {
            // Given
            Official official = new Official();
            official.setId(1L);
            official.setIsArchived(false);

            given(officialRepository.findById(1L)).willReturn(Optional.of(official));
            given(officialRepository.save(any(Official.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            officialService.archiveOfficial(1L);

            // Then
            assertThat(official.getIsArchived()).isTrue();
            then(officialRepository).should().save(official);
        }

        @Test
        @DisplayName("givenExistingId_whenDeleteOfficial_thenInvokeRepositoryDelete")
        void givenExistingId_whenDeleteOfficial_thenInvokeRepositoryDelete() {
            // When
            officialService.deleteOfficial(2L);

            // Then
            then(officialRepository).should().deleteById(2L);
        }
    }
}
