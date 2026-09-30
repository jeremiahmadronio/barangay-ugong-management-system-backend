package com.barangay.contentmanagement.service;

import com.barangay.contentmanagement.dto.OfficialDto;
import com.barangay.contentmanagement.model.Official;
import com.barangay.contentmanagement.repository.OfficialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OfficialService {

    private final OfficialRepository officialRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<Official> getPublicOfficials() {
        return officialRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public List<Official> getAllOfficials() {
        return officialRepository.findAllActive();
    }

    public Official createOfficial(OfficialDto dto) {
        Official official = new Official();
        official.setName(dto.getName());
        official.setPosition(dto.getPosition());
        official.setType(dto.getType());

        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            official.setImageUrl(fileStorageService.storeFile(dto.getImage()));
        }

        return officialRepository.save(official);
    }

    public Official updateOfficial(Long id, OfficialDto dto, boolean removeImage) {
        Official official = officialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Official not found with id: " + id));

        official.setName(dto.getName());
        official.setPosition(dto.getPosition());
        official.setType(dto.getType());
        official.setUpdatedAt(LocalDateTime.now());

        if (removeImage || dto.isRemoveImage()) {
            official.setImageUrl(null);
        } else if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            official.setImageUrl(fileStorageService.storeFile(dto.getImage()));
        }

        return officialRepository.save(official);
    }

    public void deleteOfficial(Long id) {
        officialRepository.deleteById(id);
    }

    public void archiveOfficial(Long id) {
        Official official = officialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Official not found with id: " + id));
        official.setIsArchived(true);
        officialRepository.save(official);
    }
}
