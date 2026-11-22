package com.barangay.contentmanagement.service;

import com.barangay.contentmanagement.dto.AnnouncementDto;
import com.barangay.contentmanagement.model.Announcement;
import com.barangay.contentmanagement.repository.AnnouncementRepository;
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
 * BDD Unit tests for {@link AnnouncementService}.
 * Verifies public/admin retrieval, creation, update, and lifecycle management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnnouncementService BDD Tests")
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private AnnouncementService announcementService;

    @Nested
    @DisplayName("Retrieval Tests")
    class RetrievalTests {

        @Test
        @DisplayName("givenActiveAnnouncements_whenGetPublicAnnouncements_thenReturnPublishedOnly")
        void givenActiveAnnouncements_whenGetPublicAnnouncements_thenReturnPublishedOnly() {
            // Given
            Announcement a = new Announcement();
            a.setId(1L);
            a.setTitle("Community Cleanup");
            a.setPublished(true);

            given(announcementRepository.findPublicActive()).willReturn(List.of(a));

            // When
            List<Announcement> result = announcementService.getPublicAnnouncements();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Community Cleanup");
            then(announcementRepository).should().findPublicActive();
        }

        @Test
        @DisplayName("givenAllActiveAnnouncements_whenGetAllAnnouncements_thenReturnList")
        void givenAllActiveAnnouncements_whenGetAllAnnouncements_thenReturnList() {
            // Given
            Announcement a = new Announcement();
            a.setId(2L);
            a.setTitle("Barangay Assembly");

            given(announcementRepository.findAllActive()).willReturn(List.of(a));

            // When
            List<Announcement> result = announcementService.getAllAnnouncements();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Barangay Assembly");
            then(announcementRepository).should().findAllActive();
        }
    }

    @Nested
    @DisplayName("Create and Update Tests")
    class CreateAndUpdateTests {

        @Test
        @DisplayName("givenValidDtoWithImage_whenCreateAnnouncement_thenSaveAndReturnEntity")
        void givenValidDtoWithImage_whenCreateAnnouncement_thenSaveAndReturnEntity() {
            // Given
            AnnouncementDto dto = new AnnouncementDto();
            dto.setTitle("Health Center Advisory");
            dto.setCategory("Health");
            dto.setContent("Free vaccines available.");

            MockMultipartFile file = new MockMultipartFile("image", "health.png", "image/png", "fake-image".getBytes());
            dto.setImage(file);

            given(fileStorageService.storeFile(file)).willReturn("/uploads/health.png");
            given(announcementRepository.save(any(Announcement.class))).willAnswer(invocation -> {
                Announcement saved = invocation.getArgument(0);
                saved.setId(10L);
                return saved;
            });

            // When
            Announcement result = announcementService.createAnnouncement(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getImageUrl()).isEqualTo("/uploads/health.png");
            then(announcementRepository).should().save(any(Announcement.class));
        }

        @Test
        @DisplayName("givenNonExistentId_whenUpdateAnnouncement_thenThrowRuntimeException")
        void givenNonExistentId_whenUpdateAnnouncement_thenThrowRuntimeException() {
            // Given
            AnnouncementDto dto = new AnnouncementDto();
            given(announcementRepository.findById(999L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> announcementService.updateAnnouncement(999L, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Announcement not found with id: 999");
        }
    }

    @Nested
    @DisplayName("Lifecycle Management Tests")
    class LifecycleManagementTests {

        @Test
        @DisplayName("givenExistingAnnouncement_whenTogglePublish_thenInvertPublicationState")
        void givenExistingAnnouncement_whenTogglePublish_thenInvertPublicationState() {
            // Given
            Announcement a = new Announcement();
            a.setId(1L);
            a.setPublished(true);

            given(announcementRepository.findById(1L)).willReturn(Optional.of(a));
            given(announcementRepository.save(any(Announcement.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            Announcement result = announcementService.togglePublish(1L);

            // Then
            assertThat(result.isPublished()).isFalse();
            then(announcementRepository).should().save(a);
        }

        @Test
        @DisplayName("givenExistingAnnouncement_whenArchiveAnnouncement_thenSetArchivedTrue")
        void givenExistingAnnouncement_whenArchiveAnnouncement_thenSetArchivedTrue() {
            // Given
            Announcement a = new Announcement();
            a.setId(1L);
            a.setIsArchived(false);

            given(announcementRepository.findById(1L)).willReturn(Optional.of(a));
            given(announcementRepository.save(any(Announcement.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            Announcement result = announcementService.archiveAnnouncement(1L);

            // Then
            assertThat(result.getIsArchived()).isTrue();
            then(announcementRepository).should().save(a);
        }

        @Test
        @DisplayName("givenExistingId_whenDeleteAnnouncement_thenInvokeRepositoryDelete")
        void givenExistingId_whenDeleteAnnouncement_thenInvokeRepositoryDelete() {
            // When
            announcementService.deleteAnnouncement(5L);

            // Then
            then(announcementRepository).should().deleteById(5L);
        }
    }
}
