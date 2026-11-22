package com.barangay.contentmanagement.service;

import com.barangay.contentmanagement.dto.ServiceDto;
import com.barangay.contentmanagement.model.BarangayService;
import com.barangay.contentmanagement.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing barangay community services, documentation requirements, and fees.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BarangayServiceService {

    private final ServiceRepository serviceRepository;

    /**
     * Retrieves active published services for public catalog viewing.
     *
     * @return list of published services
     */
    @Transactional(readOnly = true)
    public List<BarangayService> getPublicServices() {
        return serviceRepository.findPublicActive();
    }

    /**
     * Retrieves all active services for admin management.
     *
     * @return list of all active services
     */
    @Transactional(readOnly = true)
    public List<BarangayService> getAllServices() {
        return serviceRepository.findAllActive();
    }

    /**
     * Creates and stores a new barangay service entry.
     *
     * @param dto service details
     * @return created service
     */
    public BarangayService createService(ServiceDto dto) {
        BarangayService service = new BarangayService();
        mapDtoToEntity(dto, service);
        return serviceRepository.save(service);
    }

    /**
     * Updates an existing barangay service record.
     *
     * @param id  service identifier
     * @param dto updated details
     * @return updated service
     */
    public BarangayService updateService(Long id, ServiceDto dto) {
        BarangayService service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));

        mapDtoToEntity(dto, service);
        return serviceRepository.save(service);
    }

    /**
     * Permanently deletes a service record.
     *
     * @param id service identifier
     */
    public void deleteService(Long id) {
        serviceRepository.deleteById(id);
    }

    /**
     * Toggles the publication visibility of a service entry.
     *
     * @param id service identifier
     * @return updated service
     */
    public BarangayService togglePublish(Long id) {
        BarangayService service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        service.setIsPublished(service.getIsPublished() != null ? !service.getIsPublished() : true);
        return serviceRepository.save(service);
    }

    /**
     * Archives a service record.
     *
     * @param id service identifier
     * @return archived service
     */
    public BarangayService archiveService(Long id) {
        BarangayService service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        service.setIsArchived(true);
        return serviceRepository.save(service);
    }

    private void mapDtoToEntity(ServiceDto dto, BarangayService entity) {
        entity.setIconName(dto.getIconName());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setFee(dto.getFee());
        entity.setProcessingTime(dto.getProcessingTime());
        entity.setLocation(dto.getLocation());
        entity.setOperatingDays(dto.getOperatingDays());
        entity.setOperatingHours(dto.getOperatingHours());
        entity.setType(dto.getType() != null ? dto.getType() : "DOCUMENT");
        entity.setRequirements(dto.getRequirements());
        entity.setSteps(dto.getSteps());
        entity.setClaimProcess(dto.getClaimProcess());
    }
}
