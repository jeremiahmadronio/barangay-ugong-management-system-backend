package com.barangay.contentmanagement.service;

import com.barangay.contentmanagement.dto.OfficialDto;
import com.barangay.contentmanagement.model.Official;
import com.barangay.contentmanagement.repository.OfficialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing barangay leadership roster and official profiles.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OfficialService {

    private final OfficialRepository officialRepository;
    private final FileStorageService fileStorageService;

    /**
     * Retrieves active officials for public directory views.
     *
     * @return list of active officials
     */
    @Transactional(readOnly = true)
    public List<Official> getPublicOfficials() {
        return officialRepository.findAllActive();
    }

    /**
     * Retrieves all active officials for administration.
     *
     * @return list of active officials
     */
    @Transactional(readOnly = true)
    public List<Official> getAllOfficials() {
        return officialRepository.findAllActive();
    }

    /**
     * Creates and stores a new official entry with optional profile photo.
     *
     * @param dto official details and photo
     * @return created official
     */
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

    /**
     * Updates an existing official profile.
     *
     * @param id          official identifier
     * @param dto         updated official details
     * @param removeImage flag indicating whether photo is removed
     * @return updated official
     */
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

    /**
     * Permanently deletes an official record.
     *
     * @param id official identifier
     */
    public void deleteOfficial(Long id) {
        officialRepository.deleteById(id);
    }

    /**
     * Archives an official record.
     *
     * @param id official identifier
     */
    public void archiveOfficial(Long id) {
        Official official = officialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Official not found with id: " + id));
        official.setIsArchived(true);
        officialRepository.save(official);
    }
}
