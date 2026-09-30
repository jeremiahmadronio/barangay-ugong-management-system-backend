package com.barangay.contentmanagement.service;

import com.barangay.contentmanagement.dto.ServiceDto;
import com.barangay.contentmanagement.model.BarangayService;
import com.barangay.contentmanagement.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BarangayServiceService {

    private final ServiceRepository serviceRepository;

    @Transactional(readOnly = true)
    public List<BarangayService> getPublicServices() {
        return serviceRepository.findPublicActive();
    }

    @Transactional(readOnly = true)
    public List<BarangayService> getAllServices() {
        return serviceRepository.findAllActive();
    }

    public BarangayService createService(ServiceDto dto) {
        BarangayService service = new BarangayService();
        mapDtoToEntity(dto, service);
        return serviceRepository.save(service);
    }

    public BarangayService updateService(Long id, ServiceDto dto) {
        BarangayService service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));

        mapDtoToEntity(dto, service);
        return serviceRepository.save(service);
    }

    public void deleteService(Long id) {
        serviceRepository.deleteById(id);
    }

    public BarangayService togglePublish(Long id) {
        BarangayService service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
        service.setIsPublished(service.getIsPublished() != null ? !service.getIsPublished() : true);
        return serviceRepository.save(service);
    }

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
