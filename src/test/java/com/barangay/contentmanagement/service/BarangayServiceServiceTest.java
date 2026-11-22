package com.barangay.contentmanagement.service;

import com.barangay.contentmanagement.dto.ServiceDto;
import com.barangay.contentmanagement.model.BarangayService;
import com.barangay.contentmanagement.repository.ServiceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * BDD Unit tests for {@link BarangayServiceService}.
 * Verifies catalog queries, service registration, updates, and lifecycle operations.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BarangayServiceService BDD Tests")
class BarangayServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private BarangayServiceService barangayServiceService;

    @Nested
    @DisplayName("Catalog Retrieval Tests")
    class CatalogRetrievalTests {

        @Test
        @DisplayName("givenPublicServices_whenGetPublicServices_thenReturnPublishedActiveList")
        void givenPublicServices_whenGetPublicServices_thenReturnPublishedActiveList() {
            // Given
            BarangayService service = new BarangayService();
            service.setId(1L);
            service.setTitle("Barangay Clearance");
            service.setIsPublished(true);

            given(serviceRepository.findPublicActive()).willReturn(List.of(service));

            // When
            List<BarangayService> result = barangayServiceService.getPublicServices();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Barangay Clearance");
            then(serviceRepository).should().findPublicActive();
        }

        @Test
        @DisplayName("givenAllServices_whenGetAllServices_thenReturnAllActiveList")
        void givenAllServices_whenGetAllServices_thenReturnAllActiveList() {
            // Given
            BarangayService service = new BarangayService();
            service.setId(2L);
            service.setTitle("Certificate of Residency");

            given(serviceRepository.findAllActive()).willReturn(List.of(service));

            // When
            List<BarangayService> result = barangayServiceService.getAllServices();

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Certificate of Residency");
            then(serviceRepository).should().findAllActive();
        }
    }

    @Nested
    @DisplayName("Create and Update Tests")
    class CreateAndUpdateTests {

        @Test
        @DisplayName("givenValidServiceDto_whenCreateService_thenSaveAndReturnService")
        void givenValidServiceDto_whenCreateService_thenSaveAndReturnService() {
            // Given
            ServiceDto dto = new ServiceDto();
            dto.setIconName("file-text");
            dto.setTitle("Business Clearance");
            dto.setDescription("Requirements for business operation");
            dto.setFee("500.00");
            dto.setType("DOCUMENT");

            given(serviceRepository.save(any(BarangayService.class))).willAnswer(invocation -> {
                BarangayService saved = invocation.getArgument(0);
                saved.setId(10L);
                return saved;
            });

            // When
            BarangayService result = barangayServiceService.createService(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(10L);
            assertThat(result.getTitle()).isEqualTo("Business Clearance");
            assertThat(result.getType()).isEqualTo("DOCUMENT");
            then(serviceRepository).should().save(any(BarangayService.class));
        }

        @Test
        @DisplayName("givenNonExistentId_whenUpdateService_thenThrowRuntimeException")
        void givenNonExistentId_whenUpdateService_thenThrowRuntimeException() {
            // Given
            ServiceDto dto = new ServiceDto();
            given(serviceRepository.findById(999L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> barangayServiceService.updateService(999L, dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Service not found with id: 999");
        }
    }

    @Nested
    @DisplayName("Lifecycle Management Tests")
    class LifecycleManagementTests {

        @Test
        @DisplayName("givenExistingService_whenTogglePublish_thenInvertPublishState")
        void givenExistingService_whenTogglePublish_thenInvertPublishState() {
            // Given
            BarangayService service = new BarangayService();
            service.setId(1L);
            service.setIsPublished(true);

            given(serviceRepository.findById(1L)).willReturn(Optional.of(service));
            given(serviceRepository.save(any(BarangayService.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            BarangayService result = barangayServiceService.togglePublish(1L);

            // Then
            assertThat(result.getIsPublished()).isFalse();
            then(serviceRepository).should().save(service);
        }

        @Test
        @DisplayName("givenExistingService_whenArchiveService_thenSetArchivedTrue")
        void givenExistingService_whenArchiveService_thenSetArchivedTrue() {
            // Given
            BarangayService service = new BarangayService();
            service.setId(1L);
            service.setIsArchived(false);

            given(serviceRepository.findById(1L)).willReturn(Optional.of(service));
            given(serviceRepository.save(any(BarangayService.class))).willAnswer(invocation -> invocation.getArgument(0));

            // When
            BarangayService result = barangayServiceService.archiveService(1L);

            // Then
            assertThat(result.getIsArchived()).isTrue();
            then(serviceRepository).should().save(service);
        }
    }
}
