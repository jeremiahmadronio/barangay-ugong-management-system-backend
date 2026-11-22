package com.barangay.contentmanagement.service;

import com.barangay.contentmanagement.dto.AnnouncementDto;
import com.barangay.contentmanagement.model.Announcement;
import com.barangay.contentmanagement.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Service handling portal announcements and multi-image attachments.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final FileStorageService fileStorageService;

    /**
     * Retrieves all published, unarchived announcements for public views.
     *
     * @return list of published announcements
     */
    @Transactional(readOnly = true)
    public List<Announcement> getPublicAnnouncements() {
        return announcementRepository.findPublicActive();
    }

    /**
     * Retrieves all active announcements for administrative management.
     *
     * @return list of all active announcements
     */
    @Transactional(readOnly = true)
    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAllActive();
    }

    /**
     * Creates and stores a new announcement along with primary and additional images.
     *
     * @param dto announcement details and files
     * @return persisted announcement
     */
    public Announcement createAnnouncement(AnnouncementDto dto) {
        Announcement announcement = new Announcement();
        mapDtoToEntity(dto, announcement);

        if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            String imageUrl = fileStorageService.storeFile(dto.getImage());
            announcement.setImageUrl(imageUrl);
        }

        if (dto.getAdditionalImageFiles() != null) {
            for (MultipartFile file : dto.getAdditionalImageFiles()) {
                if (file != null && !file.isEmpty()) {
                    String url = fileStorageService.storeFile(file);
                    announcement.getAdditionalImages().add(url);
                }
            }
        }

        return announcementRepository.save(announcement);
    }

    /**
     * Updates an existing announcement record and image links.
     *
     * @param id  announcement identifier
     * @param dto updated announcement details
     * @return updated announcement
     */
    public Announcement updateAnnouncement(Long id, AnnouncementDto dto) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found with id: " + id));

        mapDtoToEntity(dto, announcement);

        if (dto.isRemoveImage()) {
            announcement.setImageUrl(null);
        } else if (dto.getImage() != null && !dto.getImage().isEmpty()) {
            String imageUrl = fileStorageService.storeFile(dto.getImage());
            announcement.setImageUrl(imageUrl);
        }

        if (dto.getAdditionalImageFiles() != null) {
            for (MultipartFile file : dto.getAdditionalImageFiles()) {
                if (file != null && !file.isEmpty()) {
                    String url = fileStorageService.storeFile(file);
                    announcement.getAdditionalImages().add(url);
                }
            }
        }

        return announcementRepository.save(announcement);
    }

    /**
     * Deletes an announcement record by identifier.
     *
     * @param id announcement identifier
     */
    public void deleteAnnouncement(Long id) {
        announcementRepository.deleteById(id);
    }

    /**
     * Toggles published state of an announcement.
     *
     * @param id announcement identifier
     * @return updated announcement
     */
    public Announcement togglePublish(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found with id: " + id));
        announcement.setPublished(!announcement.isPublished());
        return announcementRepository.save(announcement);
    }

    /**
     * Archives an announcement record.
     *
     * @param id announcement identifier
     * @return archived announcement
     */
    public Announcement archiveAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found with id: " + id));
        announcement.setIsArchived(true);
        return announcementRepository.save(announcement);
    }

    private void mapDtoToEntity(AnnouncementDto dto, Announcement entity) {
        entity.setCategory(dto.getCategory());
        entity.setCategoryColor(dto.getCategoryColor() != null ? dto.getCategoryColor() : "bg-blue-500");
        entity.setTitle(dto.getTitle());
        entity.setExcerpt(dto.getExcerpt());
        entity.setAuthor(dto.getAuthor());
        entity.setContent(dto.getContent());

        if (dto.isClearAdditionalImages()) {
            entity.setAdditionalImages(new ArrayList<>());
        } else if (dto.getAdditionalImages() != null) {
            entity.setAdditionalImages(new ArrayList<>(dto.getAdditionalImages()));
        } else if (entity.getAdditionalImages() == null) {
            entity.setAdditionalImages(new ArrayList<>());
        }
    }
}
